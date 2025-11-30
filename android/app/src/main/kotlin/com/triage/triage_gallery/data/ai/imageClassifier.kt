package com.triage.triage_gallery.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.collection.emptyFloatList
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.util.PriorityQueue

/**
 * Clase ayudante para manejar TensorFlow Lite.
 * Carga el modelo, procesa la imagen y devuelve resultados.
 */
class ImageClassifier(
    private val context: Context,
    private val modelName: String = "mi_modelo.tflite",
    private val labelName: String = "labels.txt"
) {
    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()
    // Leemos esto dinámicamente del modelo
    private var inputImageWidth: Int = 0
    private var inputImageHeight: Int = 0
    private var modelInputType: DataType = DataType.FLOAT32

    init {
        setupInterpreter()
    }

    private fun setupInterpreter() {
        try {
            val modelFile = FileUtil.loadMappedFile(context, modelName)
            val options = Interpreter.Options()
            options.setNumThreads(4)

            interpreter = Interpreter(modelFile, options)
            labels = FileUtil.loadLabels(context, labelName)

            // --- AUTO-CONFIGURACIÓN ---
            // Le preguntamos al modelo qué tamaño y tipo de datos espera
            val inputTensor = interpreter?.getInputTensor(0)
            val inputShape = inputTensor?.shape() // [1, 224, 224, 3]

            if (inputShape != null) {
                inputImageHeight = inputShape[1]
                inputImageWidth = inputShape[2]
                modelInputType = inputTensor.dataType()

                Log.d("TRIAGE_AI", "🧠 Modelo cargado: Espera ${inputImageWidth}x${inputImageHeight} en formato $modelInputType")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    data class Recognition(
        val label: String,
        val confidence: Float
    )

    fun classify(imagePath: String): List<Recognition> {
        if (interpreter == null) setupInterpreter()
        val results = ArrayList<Recognition>()

        try {
            val bitmap = loadBitmap(imagePath)
            if (bitmap == null) {
                Log.e("TRIAGE_AI", "❌ No se pudo cargar bitmap: $imagePath")
                return emptyList()
            }

            // 1. Procesador de Imagen
            val imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(inputImageHeight, inputImageWidth, ResizeOp.ResizeMethod.BILINEAR))
                // IMPORTANTE: Si el modelo es Float, a veces necesita normalización (0-255 a 0-1)
                // Pero los modelos de Transfer Learning de Keras suelen llevar la normalización DENTRO.
                // Probamos sin NormalizeOp primero.
                .build()

            // 2. Crear TensorImage con el tipo CORRECTO (Detectado automáticamente)
            var tensorImage = TensorImage(modelInputType)
            tensorImage.load(bitmap)
            tensorImage = imageProcessor.process(tensorImage)

            // 3. Output Buffer (Flexible)
            val outputTensor = interpreter?.getOutputTensor(0)
            val outputShape = outputTensor?.shape() // [1, 7]
            val outputType = outputTensor?.dataType() ?: DataType.FLOAT32

            val outputBuffer = TensorBuffer.createFixedSize(outputShape, outputType)

            // 4. Ejecutar
            interpreter?.run(tensorImage.buffer, outputBuffer.buffer.rewind())

            // 5. Leer resultados
            // Si el output es INT8/UINT8, hay que des-cuantizar, pero si es FLOAT sale directo.
            val probabilities = outputBuffer.floatArray

            val pq = PriorityQueue<Recognition>(3) { o1, o2 ->
                o2.confidence.compareTo(o1.confidence)
            }

            for (i in probabilities.indices) {
                // Si el modelo saca 0-255 (Quant), dividimos. Si saca 0-1 (Float), usamos directo.
                var confidence = probabilities[i]
                if (outputType == DataType.UINT8) {
                    confidence /= 255.0f
                }

                // Umbral bajo para debug
                if (confidence > 0.10) {
                    if (i < labels.size) {
                        pq.add(Recognition(labels[i], confidence))
                    }
                }
            }

            for (i in 0 until minOf(3, pq.size)) {
                results.add(pq.poll())
            }

        } catch (e: Exception) {
            Log.e("TRIAGE_AI", "❌ Error clasificando: ${e.message}")
            e.printStackTrace()
        }

        return results
    }

    private fun loadBitmap(path: String): Bitmap? {
        return try {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(path, options)

            val targetSize = 224
            var scale = 1
            while (options.outWidth / scale / 2 >= targetSize &&
                options.outHeight / scale / 2 >= targetSize) {
                scale *= 2
            }

            options.inJustDecodeBounds = false
            options.inSampleSize = scale
            options.inPreferredConfig = Bitmap.Config.ARGB_8888

            BitmapFactory.decodeFile(path, options)
        } catch (e: Exception) {
            null
        }
    }
}