package com.example.engine

import com.example.model.DifficultyTier
import com.example.model.GridPoint
import com.example.model.LevelCategory

object ShapeSilhouettes {

    enum class RegionType {
        PRIMARY_CONTOUR,
        SECONDARY_CONTOUR,
        INTERNAL_FILL,
        FEATURE_DETAIL
    }

    data class StructuralRegion(
        val type: RegionType,
        val description: String,
        val weight: Float = 1.0f
    )

    data class ShapeDef(
        val name: String,
        val category: LevelCategory,
        val grid: List<String>,
        val density: Float = 0.85f,
        val normalizedWidth: Float = 1.0f,
        val normalizedHeight: Float = 1.0f,
        val anchorPoints: List<GridPoint> = emptyList(),
        val structuralRegions: List<StructuralRegion> = listOf(
            StructuralRegion(RegionType.PRIMARY_CONTOUR, "Perimeter boundary", 1.2f),
            StructuralRegion(RegionType.SECONDARY_CONTOUR, "Sub-perimeter contour", 1.0f),
            StructuralRegion(RegionType.INTERNAL_FILL, "Internal interlocking core", 0.9f)
        ),
        val difficultyRange: ClosedRange<Int> = 1..1000
    )

    val ALL_SHAPES: List<ShapeDef> by lazy {
        listOf(
            // 1. SYMBOLS & ICONS
            ShapeDef("True Heart", LevelCategory.SYMBOLS, listOf(
                " ###   ### ",
                "##### #####",
                "###########",
                " ######### ",
                "  #######  ",
                "   #####   ",
                "    ###    ",
                "     #     "
            )),
            ShapeDef("Shining Star", LevelCategory.SYMBOLS, listOf(
                "    #    ",
                "   ###   ",
                "#########",
                "  #####  ",
                " ####### ",
                "##     ##"
            )),
            ShapeDef("Diamond Gem", LevelCategory.SYMBOLS, listOf(
                "  #####  ",
                " ####### ",
                "#########",
                " ####### ",
                "  #####  ",
                "   ###   ",
                "    #    "
            )),
            ShapeDef("Infinity Loop", LevelCategory.SYMBOLS, listOf(
                " ###   ### ",
                "#   # #   #",
                " ### # ### ",
                "#   # #   #",
                " ###   ### "
            )),
            ShapeDef("Lightning Bolt", LevelCategory.SYMBOLS, listOf(
                "   ##",
                "  ## ",
                " ##  ",
                "#### ",
                "  ## ",
                " ##  ",
                "##   "
            )),
            ShapeDef("Happy Smiley", LevelCategory.SYMBOLS, listOf(
                "  #####  ",
                " ####### ",
                "## # # ##",
                "#########",
                "## # # ##",
                " # ### # ",
                "  #####  "
            )),

            // 2. ANIMALS
            ShapeDef("Tropical Fish", LevelCategory.ANIMALS, listOf(
                "   ###   ",
                "  #####  ",
                " ####### ",
                "#########",
                " ####### ",
                "  #####  ",
                "   # #   ",
                "  ## ##  "
            )),
            ShapeDef("Playful Cat", LevelCategory.ANIMALS, listOf(
                "#     #",
                "### ###",
                " ##### ",
                "  ###  ",
                " ##### ",
                "#######",
                "#######",
                "# ### #",
                "  # #  "
            )),
            ShapeDef("Graceful Butterfly", LevelCategory.ANIMALS, listOf(
                " ###   ### ",
                "##### #####",
                "###########",
                " ######### ",
                "  #######  ",
                "   #####   ",
                "    ###    ",
                "     #     "
            )),
            ShapeDef("Flying Bird", LevelCategory.ANIMALS, listOf(
                "       #       ",
                "     #####     ",
                "   #########   ",
                " ############# ",
                "   ### ###     ",
                "     #   #     "
            )),
            ShapeDef("Lucky Rabbit", LevelCategory.ANIMALS, listOf(
                " #   # ",
                " #   # ",
                " #   # ",
                " ##### ",
                "#######",
                " ##### ",
                "  ###  ",
                " ##### ",
                "#######"
            )),
            ShapeDef("Majestic Elephant", LevelCategory.ANIMALS, listOf(
                "  #####   ",
                " #######  ",
                "######### ",
                "######### ",
                "### # # # ",
                "### # # # "
            )),
            ShapeDef("Playful Dog", LevelCategory.ANIMALS, listOf(
                " #     # ",
                " ### ### ",
                "  #####  ",
                "  #####  ",
                "   ###   ",
                "  #####  ",
                " ####### ",
                " ####### ",
                "  #   #  "
            )),
            ShapeDef("Mighty Lion", LevelCategory.ANIMALS, listOf(
                "  #####  ",
                " ####### ",
                "### # ###",
                " ####### ",
                "  #####  ",
                " ####### ",
                "#########",
                " #     # "
            )),
            ShapeDef("Wild Horse", LevelCategory.ANIMALS, listOf(
                "   ### ",
                "  #### ",
                " ##### ",
                "  #### ",
                " ##### ",
                "#######",
                " #   # ",
                " #   # "
            )),
            ShapeDef("Wise Owl", LevelCategory.ANIMALS, listOf(
                " #   # ",
                "#######",
                "## # ##",
                "#######",
                "#######",
                " ##### ",
                "  ###  ",
                "  # #  "
            )),
            ShapeDef("Coiled Snake", LevelCategory.ANIMALS, listOf(
                "  #### ",
                " ##  ##",
                "  #  ##",
                " ##  # ",
                "###### ",
                " ####  "
            )),
            ShapeDef("Sea Turtle", LevelCategory.ANIMALS, listOf(
                "   #   ",
                "  ###  ",
                " #######",
                "#########",
                " #######",
                "  ###  ",
                "  # #  "
            )),
            ShapeDef("Playful Dolphin", LevelCategory.ANIMALS, listOf(
                "     ##  ",
                "   ####  ",
                "  ###### ",
                " ####### ",
                "######   ",
                " ###     ",
                "  #  ##  "
            )),
            ShapeDef("Clever Fox", LevelCategory.ANIMALS, listOf(
                "#     #",
                "##   ##",
                " ##### ",
                "  ###  ",
                "   #   ",
                "  ###  ",
                " ##### ",
                "#######",
                "  # #  "
            )),
            ShapeDef("Howling Wolf", LevelCategory.ANIMALS, listOf(
                "   ##  ",
                "  #### ",
                " ######",
                "  #### ",
                " ##### ",
                "#######",
                "###### ",
                " #   # "
            )),
            ShapeDef("Fierce Tiger", LevelCategory.ANIMALS, listOf(
                " #   # ",
                "#######",
                "## # ##",
                "#######",
                " ##### ",
                "#######",
                " # # # ",
                " #   # "
            )),
            ShapeDef("Forest Deer", LevelCategory.ANIMALS, listOf(
                "#     #",
                " #   # ",
                "  ###  ",
                "  ###  ",
                "   #   ",
                " ##### ",
                "#######",
                " #   # ",
                " #   # "
            )),
            ShapeDef("Grizzly Bear", LevelCategory.ANIMALS, listOf(
                " #   # ",
                " ##### ",
                "#######",
                " ##### ",
                "#######",
                "#######",
                " #   # "
            )),
            ShapeDef("Soaring Eagle", LevelCategory.ANIMALS, listOf(
                "   #     #   ",
                "  ###   ###  ",
                " ##### ##### ",
                "#############",
                "    #####    ",
                "     ###     ",
                "      #      "
            )),
            ShapeDef("Dinosaur Silhouette", LevelCategory.ANIMALS, listOf(
                "    ####",
                "    ####",
                "   #####",
                "  ######",
                " #######",
                "#########",
                " #     #",
                " #     #"
            )),

            // 3. NATURE & PLANTS
            ShapeDef("Sweet Apple", LevelCategory.NATURE, listOf(
                "   #   ",
                "  ###  ",
                " ##### ",
                "#######",
                "#######",
                " ##### ",
                "  # #  "
            )),
            ShapeDef("Autumn Leaf", LevelCategory.NATURE, listOf(
                "   #   ",
                "  ###  ",
                " ##### ",
                "#######",
                " ##### ",
                "  ###  ",
                "   #   "
            )),
            ShapeDef("Blooming Flower", LevelCategory.NATURE, listOf(
                "  ###  ",
                " #######",
                "### # ###",
                " #######",
                "  ###  ",
                "   #   ",
                "  ##   ",
                "   #   "
            )),
            ShapeDef("Evergreen Tree", LevelCategory.NATURE, listOf(
                "   #   ",
                "  ###  ",
                " ##### ",
                "  ###  ",
                " ##### ",
                "#######",
                "   #   ",
                "   #   "
            )),
            ShapeDef("Forest Mushroom", LevelCategory.NATURE, listOf(
                "  #####  ",
                " ####### ",
                "#########",
                "  #####  ",
                "   ###   ",
                "   ###   ",
                "  #####  "
            )),
            ShapeDef("Radiant Sun", LevelCategory.NATURE, listOf(
                " # # # ",
                "  ###  ",
                "# ### #",
                "  ###  ",
                " # # # "
            )),
            ShapeDef("Crescent Moon", LevelCategory.NATURE, listOf(
                "  ###",
                " ####",
                "###  ",
                "##   ",
                "###  ",
                " ####",
                "  ###"
            )),
            ShapeDef("Fluffy Cloud", LevelCategory.NATURE, listOf(
                "  #####  ",
                " ####### ",
                "#########",
                " ####### "
            )),
            ShapeDef("Twin Mountains", LevelCategory.NATURE, listOf(
                "   #       ",
                "  ###   #  ",
                " ##### ### ",
                "###########"
            )),

            // 4. OBJECTS & TOOLS
            ShapeDef("Cosmic Rocket", LevelCategory.VEHICLES, listOf(
                "   #   ",
                "  ###  ",
                "  ###  ",
                " ##### ",
                " ##### ",
                "#######",
                " # # # "
            )),
            ShapeDef("City Car", LevelCategory.VEHICLES, listOf(
                "  #####  ",
                " ####### ",
                "#########",
                " ####### ",
                " # # # # "
            )),
            ShapeDef("Jet Airplane", LevelCategory.VEHICLES, listOf(
                "    #    ",
                "   ###   ",
                "  #####  ",
                "#########",
                "   ###   ",
                "  #####  "
            )),
            ShapeDef("Sail Boat", LevelCategory.VEHICLES, listOf(
                "   #  ",
                "  ##  ",
                " ###  ",
                "####  ",
                "   #  ",
                "######",
                " #### "
            )),
            ShapeDef("Royal Crown", LevelCategory.OBJECTS, listOf(
                "#  #  #",
                "#######",
                " ##### ",
                "#######",
                "#######"
            )),
            ShapeDef("Golden Key", LevelCategory.OBJECTS, listOf(
                " ### ",
                "#   #",
                " ### ",
                "  #  ",
                "  ## ",
                "  #  ",
                "  ## ",
                "  #  "
            )),
            ShapeDef("Medieval Castle", LevelCategory.OBJECTS, listOf(
                "# # # #",
                "#######",
                "#######",
                " ##### ",
                "## # ##",
                "#######"
            )),
            ShapeDef("Acoustic Guitar", LevelCategory.OBJECTS, listOf(
                "  #  ",
                "  #  ",
                " ### ",
                "#####",
                " ### ",
                "#####",
                "#####",
                " ### "
            )),
            ShapeDef("Cozy House", LevelCategory.OBJECTS, listOf(
                "   #   ",
                "  ###  ",
                " ##### ",
                "#######",
                "#######",
                "## # ##",
                "#######"
            )),
            ShapeDef("Rain Umbrella", LevelCategory.OBJECTS, listOf(
                "   #   ",
                "  ###  ",
                " ##### ",
                "#######",
                "   #   ",
                "   #   ",
                "  ##   "
            )),
            ShapeDef("Photo Camera", LevelCategory.OBJECTS, listOf(
                "  ###  ",
                "#######",
                "## # ##",
                "## # ##",
                "#######"
            )),

            // 5. LETTERS & NUMBERS
            ShapeDef("Letter A", LevelCategory.LETTERS, listOf(
                "  ###  ",
                " #   # ",
                " #   # ",
                " ##### ",
                " #   # ",
                " #   # "
            )),
            ShapeDef("Letter B", LevelCategory.LETTERS, listOf(
                " ####  ",
                " #   # ",
                " ####  ",
                " #   # ",
                " ####  "
            )),
            ShapeDef("Letter C", LevelCategory.LETTERS, listOf(
                " ##### ",
                " #     ",
                " #     ",
                " #     ",
                " ##### "
            )),
            ShapeDef("Letter S", LevelCategory.LETTERS, listOf(
                " ##### ",
                " #     ",
                " ##### ",
                "     # ",
                " ##### "
            )),
            ShapeDef("Letter M", LevelCategory.LETTERS, listOf(
                "#   #",
                "## ##",
                "# # #",
                "#   #",
                "#   #"
            )),
            ShapeDef("Letter Z", LevelCategory.LETTERS, listOf(
                "#####",
                "   # ",
                "  #  ",
                " #   ",
                "#####"
            )),
            ShapeDef("Number 0", LevelCategory.LETTERS, listOf(
                " ### ",
                "#   #",
                "#   #",
                "#   #",
                " ### "
            )),
            ShapeDef("Number 1", LevelCategory.LETTERS, listOf(
                "  #  ",
                " ##  ",
                "  #  ",
                "  #  ",
                " ### "
            )),
            ShapeDef("Number 2", LevelCategory.LETTERS, listOf(
                " ### ",
                "#   #",
                "   # ",
                "  #  ",
                "#####"
            )),
            ShapeDef("Number 8", LevelCategory.LETTERS, listOf(
                "  ###  ",
                " #   # ",
                "  ###  ",
                " #   # ",
                "  ###  "
            )),

            // 6. ABSTRACT & GEOMETRIC SHAPES
            ShapeDef("Concentric Diamond", LevelCategory.GEOMETRY, listOf(
                "    #    ",
                "   ###   ",
                "  # # #  ",
                " #  #  # ",
                "#########",
                " #  #  # ",
                "  # # #  ",
                "   ###   ",
                "    #    "
            )),
            ShapeDef("Hexagonal Web", LevelCategory.GEOMETRY, listOf(
                "  #####  ",
                " ####### ",
                "#########",
                "### # ###",
                "#########",
                " ####### ",
                "  #####  "
            )),
            ShapeDef("Spiral Nebula", LevelCategory.GEOMETRY, listOf(
                "#######",
                "#     #",
                "# ### #",
                "# # # #",
                "# #   #",
                "# #####",
                "#######"
            )),
            ShapeDef("Celtic Weave", LevelCategory.GEOMETRY, listOf(
                "## ## ##",
                "# # # # ",
                "## ## ##",
                "# # # # ",
                "## ## ##"
            )),

            // 7. MASTER / HIGH-COMPLEXITY MANDALAS
            ShapeDef("Mythic Dragon", LevelCategory.MASTER, listOf(
                "   #   #   ",
                "  ### ###  ",
                " ######### ",
                "###########",
                " ######### ",
                "  #######  ",
                "   #####   ",
                "  # ### #  ",
                " ##  #  ## "
            )),
            ShapeDef("Labyrinth Core", LevelCategory.MASTER, listOf(
                "#########",
                "# #######",
                "# #     #",
                "# # ### #",
                "# # # # #",
                "# # ### #",
                "# ##### #",
                "####### #"
            )),
            ShapeDef("Solar Flare Mandala", LevelCategory.MASTER, listOf(
                "  #   #   #  ",
                " # # ### # # ",
                "  #########  ",
                "### ##### ###",
                "  #########  ",
                " # # ### # # ",
                "  #   #   #  "
            ))
        )
    }
}

