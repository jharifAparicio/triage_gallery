package com.triage.triage_gallery.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Clase ayudante para manejar TensorFlow Lite.
 * Carga el modelo, procesa la imagen y devuelve resultados.
 */
class ImageClassifier(
    private val context: Context,
    // Usamos MobileNet V2 (modelo estándar, rápido y ligero para móviles)
    private val modelName: String = "mobilenet_v2_1.0_224_quant.tflite",
    private val labelName: String = "labels.txt"
) {

    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()

    // Configuración de imagen requerida por MobileNet
    private val imageSizeX = 224
    private val imageSizeY = 224

    init {
        setupInterpreter()
    }

    private fun setupInterpreter() {
        try {
            // 1. Cargar el modelo desde assets
            val modelFile = FileUtil.loadMappedFile(context, modelName)

            // 2. Opciones (Usar 4 hilos para ser rápido)
            val options = Interpreter.Options()
            options.setNumThreads(4)

            // 3. Crear el intérprete
            interpreter = Interpreter(modelFile, options)

            // 4. Cargar las etiquetas (nombres de las categorías)
            labels = FileUtil.loadLabels(context, labelName)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Clasifica una imagen desde una ruta de archivo.
     * Retorna un par: (Categoría Principal, Confianza 0.0-1.0)
     */
    fun classify(imagePath: String): Pair<String, Float>? {
        if (interpreter == null) setupInterpreter()

        try {
            // 1. Cargar Bitmap del disco (Reducido para ahorrar RAM)
            val bitmap = loadBitmap(imagePath) ?: return null

            // 2. Procesar imagen (Redimensionar a 224x224 que es lo que entiende la IA)
            val imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(imageSizeX, imageSizeY, ResizeOp.ResizeMethod.BILINEAR))
                // Si el modelo es flotante, se necesita normalización.
                // Para modelos cuantizados (quant), generalmente no, o es diferente.
                // MobileNet Quantizado usa 0-255 integers.
                .build()

            var tensorImage = TensorImage(org.tensorflow.lite.DataType.UINT8) // UINT8 para modelos cuantizados
            tensorImage.load(bitmap)
            tensorImage = imageProcessor.process(tensorImage)

            // 3. Preparar el contenedor de salida
            // MobileNet tiene 1001 categorías. El output es un array de probabilidades.
            val outputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 1001), org.tensorflow.lite.DataType.UINT8)

            // 4. EJECUTAR INFERENCIA (El momento mágico)
            interpreter?.run(tensorImage.buffer, outputBuffer.buffer.rewind())

            // 5. Interpretar resultados
            val probabilities = outputBuffer.floatArray // Convertimos a float para leer fácil

            // Buscamos la categoría con mayor probabilidad
            var maxScore = 0f
            var maxIndex = -1

            for (i in probabilities.indices) {
                // En modelos cuantizados, el valor es 0-255. Lo normalizamos a 0-1 aproximado si queremos porcentaje.
                // O simplemente comparamos el valor crudo.
                if (probabilities[i] > maxScore) {
                    maxScore = probabilities[i]
                    maxIndex = i
                }
            }

            if (maxIndex != -1 && maxIndex < labels.size) {
                // Retornamos (Etiqueta, Confianza Normalizada 0-1)
                return Pair(labels[maxIndex], maxScore / 255.0f)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    private fun loadBitmap(path: String): Bitmap? {
        return try {
            val options = BitmapFactory.Options()

            // 1. Primero leemos solo las dimensiones (sin cargar la imagen)
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(path, options)

            // 2. Calculamos cuánto reducirla (Factor de escala)
            // Queremos algo cercano a 224px, no 4000px
            val targetSize = 224
            var scale = 1
            while (options.outWidth / scale / 2 >= targetSize &&
                options.outHeight / scale / 2 >= targetSize) {
                scale *= 2
            }

            // 3. Cargamos la imagen reducida
            options.inJustDecodeBounds = false
            options.inSampleSize = scale // Esto ahorra muchísima RAM y CPU
            options.inPreferredConfig = Bitmap.Config.ARGB_8888

            BitmapFactory.decodeFile(path, options)
        } catch (e: Exception) {
            null
        }
    }
}