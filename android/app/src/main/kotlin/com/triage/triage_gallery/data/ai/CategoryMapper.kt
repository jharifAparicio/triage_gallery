package com.triage.triage_gallery.data.ai

object CategoryMapper {

    const val CAT_PEOPLE = "cat_people"
    const val CAT_PETS = "cat_pets"
    const val CAT_FOOD = "cat_food"
    const val CAT_NATURE = "cat_nature"
    const val CAT_DOCUMENTS = "cat_documents"
    const val CAT_VEHICLES = "cat_vehicles"
    const val CAT_OTHER = "cat_other"

    fun mapLabelToCategoryId(label: String): String {
        val l = label.lowercase()

        return when {
            // --- PERSONAS (Ropa, Accesorios, Profesiones, Equipamiento) ---
            l.contains("groom") || l.contains("gown") || l.contains("suit") ||
                    l.contains("tie") || l.contains("jean") || l.contains("shirt") ||
                    l.contains("jersey") || l.contains("cardigan") || l.contains("sweat") ||
                    l.contains("jacket") || l.contains("coat") || l.contains("vest") ||
                    l.contains("robe") || l.contains("kimono") || l.contains("skirt") ||
                    l.contains("wig") || l.contains("mask") || l.contains("sunglass") ||
                    l.contains("spectacle") || l.contains("uniform") || l.contains("bikini") ||
                    l.contains("swimming") || l.contains("pajama") || l.contains("cap") ||
                    l.contains("helmet") || l.contains("hat") || l.contains("scarf") ||
                    l.contains("lipstick") || l.contains("makeup") || l.contains("hair") ||
                    l.contains("lab coat") || l.contains("apron") || l.contains("bra") ||
                    l.contains("loafer") || l.contains("shoe") || l.contains("boot") ||
                    l.contains("scuba") || l.contains("backpack") || l.contains("umbrella") ||
                    l.contains("wallet") || l.contains("purse") || l.contains("abaya") ||
                    l.contains("academic gown") || l.contains("diaper") || l.contains("miniskirt") ||
                    l.contains("mortarboard") || l.contains("poncho") || l.contains("sarong") ||
                    l.contains("trench coat") || l.contains("turban") || l.contains("vestment")
                -> CAT_PEOPLE

            // --- MASCOTAS (Perros, Gatos, Razas domésticas) ---
            // Nota: ImageNet tiene muchísimas razas de perros.
            l.contains("cat") || l.contains("dog") || l.contains("terrier") ||
                    l.contains("retriever") || l.contains("collie") || l.contains("dalmatian") ||
                    l.contains("beagle") || l.contains("pug") || l.contains("siamese") ||
                    l.contains("chihuahua") || l.contains("bulldog") || l.contains("poodle") ||
                    l.contains("husky") || l.contains("shepherd") || l.contains("labrador") ||
                    l.contains("corgi") || l.contains("rabbit") || l.contains("hamster") ||
                    l.contains("mouse") || l.contains("guinea pig") || l.contains("spaniel") ||
                    l.contains("hound") || l.contains("pinscher") || l.contains("schnauzer") ||
                    l.contains("setter") || l.contains("sheepdog") || l.contains("mastiff") ||
                    l.contains("newfoundland") || l.contains("samoyed") || l.contains("pomeranian") ||
                    l.contains("chow") || l.contains("keeshond") || l.contains("griffon") ||
                    l.contains("malamute") || l.contains("boxer") || l.contains("rottweiler") ||
                    l.contains("doberman") || l.contains("great dane") || l.contains("saint bernard") ||
                    l.contains("lhasa") || l.contains("papillon") || l.contains("ridgeback") ||
                    l.contains("basset") || l.contains("bloodhound") || l.contains("bluetick") ||
                    l.contains("coonhound") || l.contains("foxhound") || l.contains("redbone") ||
                    l.contains("borzoi") || l.contains("wolfhound") || l.contains("greyhound") ||
                    l.contains("whippet") || l.contains("ibizan") || l.contains("elkhound") ||
                    l.contains("otterhound") || l.contains("saluki") || l.contains("deerhound") ||
                    l.contains("weimaraner") || l.contains("bullterrier") || l.contains("staffordshire") ||
                    l.contains("bedlington") || l.contains("kerry blue") || l.contains("norfolk") ||
                    l.contains("norwich") || l.contains("yorkshire") || l.contains("airedale") ||
                    l.contains("cairn") || l.contains("australian") || l.contains("dandie") ||
                    l.contains("boston bull") || l.contains("schnauzer") || l.contains("scotch") ||
                    l.contains("tibetan") || l.contains("silky") || l.contains("wheaten") ||
                    l.contains("west highland") || l.contains("pointer") || l.contains("vizsla") ||
                    l.contains("brittany") || l.contains("clumber") || l.contains("springer") ||
                    l.contains("cocker") || l.contains("sussex") || l.contains("kuvasz") ||
                    l.contains("schipperke") || l.contains("groenendael") || l.contains("malinois") ||
                    l.contains("briard") || l.contains("kelpie") || l.contains("komondor") ||
                    l.contains("bouvier") || l.contains("appenzeller") || l.contains("entlebucher") ||
                    l.contains("eskimo dog") || l.contains("affenpinscher") || l.contains("basenji") ||
                    l.contains("leonberg") || l.contains("pyrenees") || l.contains("brabancon") ||
                    l.contains("tabby") || l.contains("persian") || l.contains("egyptian")
                -> CAT_PETS

            // --- COMIDA (Frutas, Vegetales, Platos, Bebidas) ---
            l.contains("pizza") || l.contains("burger") || l.contains("bread") ||
                    l.contains("chocolate") || l.contains("cake") || l.contains("fruit") ||
                    l.contains("vegetable") || l.contains("coffee") || l.contains("menu") ||
                    l.contains("espresso") || l.contains("cup") || l.contains("bakery") ||
                    l.contains("burrito") || l.contains("taco") || l.contains("orange") ||
                    l.contains("lemon") || l.contains("banana") || l.contains("strawberry") ||
                    l.contains("broccoli") || l.contains("cucumber") || l.contains("mushroom") ||
                    l.contains("meat") || l.contains("soup") || l.contains("dish") ||
                    l.contains("plate") || l.contains("bottle") || l.contains("wine") ||
                    l.contains("bagel") || l.contains("pretzel") || l.contains("hotdog") ||
                    l.contains("potato") || l.contains("cabbage") || l.contains("cauliflower") ||
                    l.contains("zucchini") || l.contains("squash") || l.contains("artichoke") ||
                    l.contains("pepper") || l.contains("cardoon") || l.contains("granny smith") ||
                    l.contains("fig") || l.contains("pineapple") || l.contains("jackfruit") ||
                    l.contains("custard apple") || l.contains("pomegranate") || l.contains("hay") ||
                    l.contains("carbonara") || l.contains("sauce") || l.contains("dough") ||
                    l.contains("loaf") || l.contains("potpie") || l.contains("eggnog") ||
                    l.contains("guacamole") || l.contains("consomme") || l.contains("trifle") ||
                    l.contains("ice cream") || l.contains("lolly") || l.contains("beer") ||
                    l.contains("cocktail") || l.contains("confectionery") || l.contains("frying pan") ||
                    l.contains("goblet") || l.contains("rotisserie") || l.contains("saltshaker") ||
                    l.contains("toaster") || l.contains("waffle") || l.contains("whiskey") ||
                    l.contains("wok") || l.contains("spoon")
                -> CAT_FOOD

            // --- NATURALEZA (Paisajes, Animales Salvajes, Plantas) ---
            l.contains("valley") || l.contains("alp") || l.contains("volcano") ||
                    l.contains("cliff") || l.contains("coral") || l.contains("lakeside") ||
                    l.contains("seashore") || l.contains("sandbar") || l.contains("mountain") ||
                    l.contains("forest") || l.contains("sea") || l.contains("ocean") ||
                    l.contains("beach") || l.contains("flower") || l.contains("tree") ||
                    l.contains("garden") || l.contains("sky") || l.contains("shark") ||
                    l.contains("ray") || l.contains("hen") || l.contains("ostrich") ||
                    l.contains("finch") || l.contains("junco") || l.contains("bunting") ||
                    l.contains("robin") || l.contains("bulbul") || l.contains("jay") ||
                    l.contains("magpie") || l.contains("chickadee") || l.contains("kite") ||
                    l.contains("eagle") || l.contains("vulture") || l.contains("owl") ||
                    l.contains("salamander") || l.contains("newt") || l.contains("eft") ||
                    l.contains("axolotl") || l.contains("frog") || l.contains("turtle") ||
                    l.contains("terrapin") || l.contains("gecko") || l.contains("iguana") ||
                    l.contains("chameleon") || l.contains("whiptail") || l.contains("agama") ||
                    l.contains("lizard") || l.contains("dragon") || l.contains("crocodile") ||
                    l.contains("alligator") || l.contains("triceratops") || l.contains("snake") ||
                    l.contains("viper") || l.contains("diamondback") || l.contains("sidewinder") ||
                    l.contains("trilobite") || l.contains("harvestman") || l.contains("scorpion") ||
                    l.contains("spider") || l.contains("tarantula") || l.contains("tick") ||
                    l.contains("centipede") || l.contains("grouse") || l.contains("ptarmigan") ||
                    l.contains("chicken") || l.contains("peacock") || l.contains("quail") ||
                    l.contains("partridge") || l.contains("macaw") || l.contains("cockatoo") ||
                    l.contains("lorikeet") || l.contains("coucal") || l.contains("bee") ||
                    l.contains("hornbill") || l.contains("hummingbird") || l.contains("jacamar") ||
                    l.contains("toucan") || l.contains("drake") || l.contains("merganser") ||
                    l.contains("goose") || l.contains("swan") || l.contains("tusker") ||
                    l.contains("echidna") || l.contains("platypus") || l.contains("wallaby") ||
                    l.contains("koala") || l.contains("wombat") || l.contains("jellyfish") ||
                    l.contains("anemone") || l.contains("flatworm") || l.contains("nematode") ||
                    l.contains("conch") || l.contains("snail") || l.contains("slug") ||
                    l.contains("chiton") || l.contains("nautilus") || l.contains("crab") ||
                    l.contains("lobster") || l.contains("crayfish") || l.contains("isopod") ||
                    l.contains("stork") || l.contains("spoonbill") || l.contains("flamingo") ||
                    l.contains("heron") || l.contains("bittern") || l.contains("crane") ||
                    l.contains("limpkin") || l.contains("gallinule") || l.contains("coot") ||
                    l.contains("bustard") || l.contains("turnstone") || l.contains("sandpiper") ||
                    l.contains("redshank") || l.contains("dowitcher") || l.contains("oystercatcher") ||
                    l.contains("pelican") || l.contains("penguin") || l.contains("albatross") ||
                    l.contains("whale") || l.contains("dugong") || l.contains("lion") ||
                    l.contains("wolf") || l.contains("fox") || l.contains("cougar") ||
                    l.contains("lynx") || l.contains("leopard") || l.contains("jaguar") ||
                    l.contains("cheetah") || l.contains("bear") || l.contains("mongoose") ||
                    l.contains("meerkat") || l.contains("beetle") || l.contains("ladybug") ||
                    l.contains("weevil") || l.contains("fly") || l.contains("ant") ||
                    l.contains("grasshopper") || l.contains("cricket") || l.contains("stick") ||
                    l.contains("cockroach") || l.contains("mantis") || l.contains("cicada") ||
                    l.contains("leafhopper") || l.contains("lacewing") || l.contains("dragonfly") ||
                    l.contains("damselfly") || l.contains("admiral") || l.contains("ringlet") ||
                    l.contains("monarch") || l.contains("butterfly") || l.contains("starfish") ||
                    l.contains("urchin") || l.contains("hare") || l.contains("porcupine") ||
                    l.contains("squirrel") || l.contains("marmot") || l.contains("beaver") ||
                    l.contains("zebra") || l.contains("hog") || l.contains("boar") ||
                    l.contains("warthog") || l.contains("hippopotamus") || l.contains("ox") ||
                    l.contains("buffalo") || l.contains("bison") || l.contains("ram") ||
                    l.contains("bighorn") || l.contains("ibex") || l.contains("hartebeest") ||
                    l.contains("impala") || l.contains("gazelle") || l.contains("camel") ||
                    l.contains("llama") || l.contains("weasel") || l.contains("mink") ||
                    l.contains("polecat") || l.contains("ferret") || l.contains("otter") ||
                    l.contains("skunk") || l.contains("badger") || l.contains("armadillo") ||
                    l.contains("sloth") || l.contains("orangutan") || l.contains("gorilla") ||
                    l.contains("chimpanzee") || l.contains("gibbon") || l.contains("siamang") ||
                    l.contains("guenon") || l.contains("patas") || l.contains("baboon") ||
                    l.contains("macaque") || l.contains("langur") || l.contains("colobus") ||
                    l.contains("monkey") || l.contains("marmoset") || l.contains("capuchin") ||
                    l.contains("indri") || l.contains("elephant") || l.contains("panda") ||
                    l.contains("fish") || l.contains("sturgeon") || l.contains("gar") ||
                    l.contains("puffer") || l.contains("rapeseed") || l.contains("daisy") ||
                    l.contains("slipper") || l.contains("corn") || l.contains("acorn") ||
                    l.contains("hip") || l.contains("buckeye") || l.contains("fungus") ||
                    l.contains("agaric") || l.contains("gyromitra") || l.contains("stinkhorn") ||
                    l.contains("earthstar") || l.contains("bolete") || l.contains("ear") ||
                    l.contains("geyser") || l.contains("promontory")
                -> CAT_NATURE

            // --- DOCUMENTOS / OFICINA ---
            l.contains("envelope") || l.contains("carton") || l.contains("paper") ||
                    l.contains("packet") || l.contains("book") || l.contains("binder") ||
                    l.contains("notebook") || l.contains("laptop") || l.contains("monitor") ||
                    l.contains("screen") || l.contains("keyboard") || l.contains("mouse") ||
                    l.contains("desk") || l.contains("typewriter") || l.contains("rule") ||
                    l.contains("web site") || l.contains("comic book") || l.contains("crossword") ||
                    l.contains("street sign") || l.contains("traffic light") || l.contains("ballpoint") ||
                    l.contains("desktop computer") || l.contains("file") || l.contains("fountain pen") ||
                    l.contains("hard disc") || l.contains("library") || l.contains("mailbag") ||
                    l.contains("mailbox") || l.contains("pay-phone") || l.contains("pencil") ||
                    l.contains("photocopier") || l.contains("printer") || l.contains("quill") ||
                    l.contains("slide rule")
                -> CAT_DOCUMENTS

            // --- VEHÍCULOS (Transporte) ---
            l.contains("car") || l.contains("cab") || l.contains("bus") ||
                    l.contains("truck") || l.contains("bicycle") || l.contains("bike") ||
                    l.contains("wheel") || l.contains("motor") || l.contains("scooter") ||
                    l.contains("train") || l.contains("ship") || l.contains("boat") ||
                    l.contains("plane") || l.contains("racer") || l.contains("ambulance") ||
                    l.contains("barrow") || l.contains("bobsled") || l.contains("convertible") ||
                    l.contains("forklift") || l.contains("go-kart") || l.contains("golfcart") ||
                    l.contains("jeep") || l.contains("limousine") || l.contains("locomotive") ||
                    l.contains("minivan") || l.contains("moped") || l.contains("snowmobile") ||
                    l.contains("tractor") || l.contains("tricycle") || l.contains("unicycle") ||
                    l.contains("van") || l.contains("wagon") || l.contains("airliner") ||
                    l.contains("airship") || l.contains("catamaran") || l.contains("liner") ||
                    l.contains("speedboat") || l.contains("trailer") || l.contains("trolleybus") ||
                    l.contains("aircraft carrier") || l.contains("canoe") || l.contains("freight car") ||
                    l.contains("jinrikisha") || l.contains("lifeboat") || l.contains("minibus") ||
                    l.contains("missile") || l.contains("mobile home") || l.contains("model t") ||
                    l.contains("moving van") || l.contains("oxcart") || l.contains("passenger car") ||
                    l.contains("police van") || l.contains("recreational vehicle") || l.contains("schooner") ||
                    l.contains("snowplow") || l.contains("space shuttle") || l.contains("steam locomotive") ||
                    l.contains("streetcar") || l.contains("submarine") || l.contains("tank") ||
                    l.contains("tow truck") || l.contains("warplane") || l.contains("wreck") ||
                    l.contains("yawl")
                -> CAT_VEHICLES

            else -> CAT_OTHER
        }
    }

    fun getCategoryName(id: String): String {
        return when(id) {
            CAT_PEOPLE -> "Personas"
            CAT_PETS -> "Mascotas"
            CAT_FOOD -> "Comida"
            CAT_NATURE -> "Naturaleza"
            CAT_DOCUMENTS -> "Docs"
            CAT_VEHICLES -> "Vehículos"
            else -> "Otros"
        }
    }
}