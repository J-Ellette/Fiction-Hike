package com.example.data.local

import com.example.data.model.FictionalTrek
import com.example.data.model.LoreDrop
import com.example.data.model.TrekDifficulty
import com.example.data.model.Waypoint

object CuratedTreksCatalog {
    val treks: List<FictionalTrek> = listOf(
        FictionalTrek(
            id = "lotr_frodo_mount_doom",
            title = "Path to Mount Doom",
            universe = "Lord of the Rings",
            characterName = "Frodo Baggins",
            characterRole = "Ring-bearer of the Shire",
            description = "Follow the perilous fellowship route from rolling Shire pastures across high mountain passes to the volcanic ash plains of Mordor.",
            difficulty = TrekDifficulty.LEGENDARY,
            estimatedKm = 18.0,
            elevationGainM = 1420,
            accentColorHex = "#E8B84B",
            waypoints = listOf(
                Waypoint(0.00, 0.00, 95.0, "Bag End Gate"),
                Waypoint(0.16, 0.08, 130.0, "Bree Crossroads"),
                Waypoint(0.32, 0.22, 390.0, "Weathertop Ridge"),
                Waypoint(0.48, 0.14, 520.0, "Rivendell Valley"),
                Waypoint(0.56, -0.08, 760.0, "Caradhras & Moria"),
                Waypoint(0.70, -0.20, 290.0, "Lothlorien Canopy"),
                Waypoint(0.84, -0.10, 410.0, "Dead Marshes"),
                Waypoint(0.95, 0.04, 880.0, "Cirith Ungol"),
                Waypoint(1.00, 0.24, 1340.0, "Mount Doom Crater")
            ),
            loreDrops = listOf(
                LoreDrop(
                    id = "frodo_bag_end",
                    title = "Departure at Dawn",
                    category = "Behind the Scenes",
                    quote = "Step onto the road ahead and see where the trail carries your boots.",
                    content = "Filmed near Matamata in New Zealand's Waikato region, the Shire sets were built a year before cameras rolled so gardens and hedges would look authentically overgrown.",
                    waypointIndex = 0
                ),
                LoreDrop(
                    id = "frodo_weathertop",
                    title = "Watchtower of Amon Sul",
                    category = "Trivia",
                    quote = "Cold winds sweep the ruined stone ring atop the lonely hill.",
                    content = "Amon Sul once held one of the seven seeing-stones of the northern kingdom, making it the highest tactical vantage point along the Great East Road.",
                    waypointIndex = 2
                ),
                LoreDrop(
                    id = "frodo_moria",
                    title = "Khazad-dum Crossing",
                    category = "Lore",
                    quote = "Deep stone pillars vanish into shadow beneath the Misty Mountains.",
                    content = "The underground span was modeled after vast Gothic cathedrals carved directly inside living granite mountains, spanning nearly forty miles end to end.",
                    waypointIndex = 4
                ),
                LoreDrop(
                    id = "frodo_marshes",
                    title = "Lanterns in the Mire",
                    category = "Trivia",
                    quote = "Step carefully between pale reed islands where phantom lights drift.",
                    content = "Concept artists drew inspiration from World War I battlefields of the Somme when designing the bleak, waterlogged expanse bordering Mordor.",
                    waypointIndex = 6
                ),
                LoreDrop(
                    id = "frodo_mount_doom",
                    title = "Cracks of Orodruin",
                    category = "Behind the Scenes",
                    quote = "Ash hangs thick above the final ascent to the forge of fire.",
                    content = "Mount Ngauruhoe in Tongariro National Park served as the towering volcanic silhouette for Orodruin, rising over 2,200 meters above sea level.",
                    waypointIndex = 8
                )
            )
        ),
        FictionalTrek(
            id = "lotr_aragorn_paths",
            title = "Ranger's March & Paths of the Dead",
            universe = "Lord of the Rings",
            characterName = "Aragorn",
            characterRole = "Heir of Isildur",
            description = "Track the swift three-day pursuit across Rohan's windswept steppes and descend beneath the White Mountains toward Pelennor.",
            difficulty = TrekDifficulty.EPIC,
            estimatedKm = 14.5,
            elevationGainM = 980,
            accentColorHex = "#3DD598",
            waypoints = listOf(
                Waypoint(0.00, 0.00, 210.0, "Amon Hen Shore"),
                Waypoint(0.22, -0.14, 340.0, "Eastemnet Plains"),
                Waypoint(0.45, -0.26, 480.0, "Fangorn Edge"),
                Waypoint(0.68, -0.10, 890.0, "Dunharrow Switchbacks"),
                Waypoint(0.82, 0.14, 410.0, "Dimholt Gorge"),
                Waypoint(1.00, 0.28, 190.0, "Pelennor Gates")
            ),
            loreDrops = listOf(
                LoreDrop(
                    id = "aragorn_three_hunters",
                    title = "Forty-Five Leagues in Three Days",
                    category = "Trivia",
                    quote = "True trackers read bent grass blades before sunrise.",
                    content = "Covering roughly 135 miles on foot in four days across rugged limestone ridges remains one of fantasy literature's most celebrated endurance feats.",
                    waypointIndex = 1
                ),
                LoreDrop(
                    id = "aragorn_dunharrow",
                    title = "Stairs of the Hold",
                    category = "Behind the Scenes",
                    quote = "Carved stone watch-figures line each switchback climbing into the mist.",
                    content = "The Putangirua Pinnacles, towering eroded gravel pillars near Wellington, provided the eerie stone canyon backdrop for the Dimholt approach.",
                    waypointIndex = 3
                ),
                LoreDrop(
                    id = "aragorn_pelennor",
                    title = "Beacon of the White City",
                    category = "Lore",
                    quote = "Seven tiers of white stone catch the first morning sun.",
                    content = "The Pelennor approaches were shot across sprawling glacial river plains near Twizel, accommodating hundreds of riders at full gallop.",
                    waypointIndex = 5
                )
            )
        ),
        FictionalTrek(
            id = "hobbit_bilbo_there_back",
            title = "There and Back Again",
            universe = "The Hobbit",
            characterName = "Bilbo Baggins",
            characterRole = "Reluctant Burglar",
            description = "Venture east from cozy Shire hedgerows through misty pine forests and barrel-run rivers to the Lonely Mountain.",
            difficulty = TrekDifficulty.MODERATE,
            estimatedKm = 11.2,
            elevationGainM = 720,
            accentColorHex = "#F48C42",
            waypoints = listOf(
                Waypoint(0.00, 0.00, 90.0, "Bag End Porch"),
                Waypoint(0.20, 0.16, 230.0, "Trollshaws Pines"),
                Waypoint(0.42, 0.28, 690.0, "High Pass Overlook"),
                Waypoint(0.60, 0.10, 310.0, "Carrock Outcrop"),
                Waypoint(0.80, 0.18, 220.0, "Mirkwood Old Road"),
                Waypoint(0.92, -0.04, 180.0, "Lake-town Docks"),
                Waypoint(1.00, 0.26, 840.0, "Lonely Mountain Door")
            ),
            loreDrops = listOf(
                LoreDrop(
                    id = "bilbo_pocket_handkerchief",
                    title = "No Time for a Handkerchief",
                    category = "Trivia",
                    quote = "Every grand expedition starts with a sudden sprint down the lane.",
                    content = "Bilbo leaves Bag End without hat, walking stick, or coin, marking his shift from cautious comfort to resourceful trailblazer.",
                    waypointIndex = 0
                ),
                LoreDrop(
                    id = "bilbo_mirkwood",
                    title = "Stay on the Forest Path",
                    category = "Lore",
                    quote = "Beneath ancient boughs, straying twenty paces means losing the compass.",
                    content = "Before darkening under ancient shadow, this vast woodland was known as Greenwood the Great and spanned hundreds of miles of old-growth canopy.",
                    waypointIndex = 4
                ),
                LoreDrop(
                    id = "bilbo_erebor",
                    title = "Thrush on the Stone",
                    category = "Behind the Scenes",
                    quote = "Last light of Durin's autumn sun reveals the hidden keyhole.",
                    content = "The Secret Door ridge scenes combined miniature architectural models with alpine plateau photography in Otago.",
                    waypointIndex = 6
                )
            )
        ),
        FictionalTrek(
            id = "got_arya_riverlands",
            title = "Riverlands Escape to Braavos",
            universe = "Game of Thrones",
            characterName = "Arya Stark",
            characterRole = "Wolf of Winterfell",
            description = "Slip north through war-torn Riverlands crossroads, burned keeps, and salt-marsh harbors to catch passage across the Narrow Sea.",
            difficulty = TrekDifficulty.EPIC,
            estimatedKm = 13.0,
            elevationGainM = 640,
            accentColorHex = "#EF6461",
            waypoints = listOf(
                Waypoint(0.00, 0.00, 60.0, "Kingsroad Gate"),
                Waypoint(0.18, 0.20, 150.0, "Crossroads Inn"),
                Waypoint(0.38, 0.08, 240.0, "Harrenhal Ruins"),
                Waypoint(0.56, 0.28, 310.0, "High Heart Hill"),
                Waypoint(0.76, 0.14, 180.0, "Twins Approach"),
                Waypoint(0.90, -0.10, 45.0, "Saltpans Harbor"),
                Waypoint(1.00, -0.22, 80.0, "Titan of Braavos")
            ),
            loreDrops = listOf(
                LoreDrop(
                    id = "arya_needle_trail",
                    title = "Small Blade, Swift Step",
                    category = "Trivia",
                    quote = "Quiet as a shadow and swift as a deer along the Kingsroad.",
                    content = "The Kingsroad stretches nearly two thousand miles from Storm's End to Castle Black, making the Riverlands crossroads the central hub of Westeros.",
                    waypointIndex = 1
                ),
                LoreDrop(
                    id = "arya_harrenhal",
                    title = "Melted Towers of Harren",
                    category = "Behind the Scenes",
                    quote = "Five blackened towers loom over the misty northern shore of Gods Eye.",
                    content = "Filmed across dramatic limestone ruins and coastal fortifications in Northern Ireland, Harrenhal's scale was modeled to dwarf every other fortress.",
                    waypointIndex = 2
                ),
                LoreDrop(
                    id = "arya_braavos",
                    title = "Iron Coin at the Harbor",
                    category = "Lore",
                    quote = "Green sea mist swirls around the bronze legs guarding the lagoon.",
                    content = "Braavos was inspired by Renaissance Venice and the ancient Colossus of Rhodes, featuring stone canals hidden behind rugged sea-mountain cliffs.",
                    waypointIndex = 6
                )
            )
        ),
        FictionalTrek(
            id = "got_jon_beyond_wall",
            title = "The King's Road & Beyond the Wall",
            universe = "Game of Thrones",
            characterName = "Jon Snow",
            characterRole = "Watcher on the Wall",
            description = "March north from Castle Black's ice escarpment through the Haunted Forest to the frozen granite ridge of the First Men.",
            difficulty = TrekDifficulty.LEGENDARY,
            estimatedKm = 16.4,
            elevationGainM = 1190,
            accentColorHex = "#7CC6FE",
            waypoints = listOf(
                Waypoint(0.00, 0.00, 220.0, "Castle Black Courtyard"),
                Waypoint(0.18, 0.24, 480.0, "Top of the Wall"),
                Waypoint(0.42, 0.36, 410.0, "Haunted Forest Trail"),
                Waypoint(0.68, 0.48, 860.0, "Fist of the First Men"),
                Waypoint(0.88, 0.32, 920.0, "Frostfangs Pass"),
                Waypoint(1.00, 0.14, 140.0, "Hardhome Cliffs")
            ),
            loreDrops = listOf(
                LoreDrop(
                    id = "jon_wall_ascent",
                    title = "Seven Hundred Feet of Glacial Ice",
                    category = "Behind the Scenes",
                    quote = "A thousand miles of blue ice separate hearthfire from wild pine.",
                    content = "Magheramorne Quarry in Northern Ireland housed the massive 40-foot elevator rig, digitally extended skyward into the iconic ice rampart.",
                    waypointIndex = 1
                ),
                LoreDrop(
                    id = "jon_fist",
                    title = "Ringwall of the First Men",
                    category = "Trivia",
                    quote = "Obsidian blades lie buried beneath fresh drifts along the ancient ridge.",
                    content = "Iceland's Vatnajokull glacier and Svinafellsjokull icefall stood in for the jagged Frostfangs and ancient summit ring.",
                    waypointIndex = 3
                ),
                LoreDrop(
                    id = "jon_hardhome",
                    title = "Sea-Fog at Hardhome",
                    category = "Lore",
                    quote = "Cold tide meets shattered timber along the Shivering Sea.",
                    content = "The abandoned whaling village sits at the tip of Storrold's Point, hemmed in by towering coastal cliffs.",
                    waypointIndex = 5
                )
            )
        ),
        FictionalTrek(
            id = "hp_horcrux_highlands",
            title = "Horcrux Hunt & Highlands Circuit",
            universe = "Harry Potter",
            characterName = "Harry Potter",
            characterRole = "Seeker of the Hollows",
            description = "Navigate misty woodland campsites, ancient village hollows, and Scottish highland lochs searching for hidden relics.",
            difficulty = TrekDifficulty.MODERATE,
            estimatedKm = 9.6,
            elevationGainM = 530,
            accentColorHex = "#D4A346",
            waypoints = listOf(
                Waypoint(0.00, 0.00, 85.0, "Grimmauld Threshold"),
                Waypoint(0.24, 0.14, 210.0, "Forest of Dean Clearing"),
                Waypoint(0.46, 0.28, 320.0, "Godric's Hollow Lane"),
                Waypoint(0.68, 0.08, 440.0, "Shell Cottage Dunes"),
                Waypoint(0.86, 0.24, 610.0, "Viaduct Glen"),
                Waypoint(1.00, 0.34, 520.0, "Black Lake Boathouse")
            ),
            loreDrops = listOf(
                LoreDrop(
                    id = "hp_dean_forest",
                    title = "Silver Doe by the Frozen Pool",
                    category = "Lore",
                    quote = "Lantern glow shimmers across frost-rimmed oaks in the quiet glade.",
                    content = "The real Forest of Dean in Gloucestershire spans 42 square miles of ancient royal woodland where J.K. Rowling set the winter camp chapters.",
                    waypointIndex = 1
                ),
                LoreDrop(
                    id = "hp_godrics",
                    title = "Stone Churchyard at Dusk",
                    category = "Trivia",
                    quote = "Snow blankets crooked timber roofs along the quiet West Country green.",
                    content = "Lavenham in Suffolk, a remarkably preserved medieval wool village, inspired the visual look of Godric's Hollow.",
                    waypointIndex = 2
                ),
                LoreDrop(
                    id = "hp_highlands",
                    title = "Steaming Bridge Over Loch Shiel",
                    category = "Behind the Scenes",
                    quote = "Mountain echoes carry across the stone arches at dusk.",
                    content = "Glenfinnan Viaduct and Loch Shiel in the Scottish Highlands appear throughout the series as the scenic approach to the castle grounds.",
                    waypointIndex = 4
                )
            )
        ),
        FictionalTrek(
            id = "stand_stu_exodus",
            title = "Exodus to Boulder Free Zone",
            universe = "The Stand",
            characterName = "Stu Redman",
            characterRole = "East Texas Survivor",
            description = "Cross empty interstates, quiet midwestern cornfields, and high Rocky Mountain passes toward Boulder's mountain sanctuary.",
            difficulty = TrekDifficulty.EPIC,
            estimatedKm = 15.2,
            elevationGainM = 1110,
            accentColorHex = "#E69F45",
            waypoints = listOf(
                Waypoint(0.00, 0.00, 140.0, "Arnette Service Station"),
                Waypoint(0.22, 0.12, 210.0, "Stovington Overpass"),
                Waypoint(0.44, 0.20, 380.0, "Lincoln Tunnel Exit"),
                Waypoint(0.64, 0.06, 460.0, "Hemingford Porch"),
                Waypoint(0.82, 0.22, 1650.0, "Boulder Flatirons"),
                Waypoint(1.00, -0.06, 620.0, "Mojave Highway 91")
            ),
            loreDrops = listOf(
                LoreDrop(
                    id = "stand_arnette",
                    title = "Quiet Pumps at Dusk",
                    category = "Lore",
                    quote = "Hot Texas asphalt hums beneath an empty prairie sky.",
                    content = "Stephen King modeled Arnette after small East Texas oil towns where silence suddenly descends when the highways clear.",
                    waypointIndex = 0
                ),
                LoreDrop(
                    id = "stand_hemingford",
                    title = "Front Porch in the Corn",
                    category = "Trivia",
                    quote = "Tall green stalks rustle beside the weathered white farmhouse.",
                    content = "Hemingford Home sits in rural Nebraska's plains, serving as the spiritual waypoint gathering survivors from coast to coast.",
                    waypointIndex = 3
                ),
                LoreDrop(
                    id = "stand_flatirons",
                    title = "Dawn Over Chautauqua",
                    category = "Behind the Scenes",
                    quote = "Slanted sandstone slabs catch first light above the mountain valley.",
                    content = "King lived in Boulder while drafting major sections of the novel, weaving real streets like Pearl Street and the Flatirons into the trail.",
                    waypointIndex = 4
                )
            )
        ),
        FictionalTrek(
            id = "stand_abigail_vigil",
            title = "Nebraska Cornfields & Mountain Vigil",
            universe = "The Stand",
            characterName = "Mother Abagail",
            characterRole = "108-Year-Old Guide",
            description = "Walk the contemplative river valleys and foothill trails connecting rural Nebraska homesteads to the Front Range foothills.",
            difficulty = TrekDifficulty.EASY,
            estimatedKm = 6.4,
            elevationGainM = 410,
            accentColorHex = "#B3E283",
            waypoints = listOf(
                Waypoint(0.00, 0.00, 390.0, "Hemingford Homestead"),
                Waypoint(0.28, 0.14, 420.0, "Platte River Crossing"),
                Waypoint(0.58, 0.08, 760.0, "Foothills Shrine"),
                Waypoint(0.84, 0.24, 1120.0, "Pine Ridge Overlook"),
                Waypoint(1.00, 0.16, 980.0, "Boulder Sanctuary")
            ),
            loreDrops = listOf(
                LoreDrop(
                    id = "abigail_hymn",
                    title = "Song Along the Dirt Road",
                    category = "Lore",
                    quote = "Old hymns echo clearly across quiet dawn pastures.",
                    content = "Mother Abagail's endurance on foot across nearly five hundred miles of plains symbolizes steadfast resolve.",
                    waypointIndex = 1
                ),
                LoreDrop(
                    id = "abigail_vigil",
                    title = "Wilderness Fast on the Ridge",
                    category = "Trivia",
                    quote = "High pines stand sentinel above the quiet stone clearing.",
                    content = "The Boulder foothills mark the turning point where scattered travelers unite into an organized frontier town.",
                    waypointIndex = 3
                )
            )
        ),
        FictionalTrek(
            id = "road_father_boy_highway",
            title = "The Ash-Covered Highway South",
            universe = "The Road",
            characterName = "The Father & The Boy",
            characterRole = "Carriers of the Fire",
            description = "Push south over mist-draped Appalachian gaps, charred timberlands, and silent coastal viaducts in search of warmer grey seas.",
            difficulty = TrekDifficulty.EPIC,
            estimatedKm = 12.6,
            elevationGainM = 890,
            accentColorHex = "#9AA5AE",
            waypoints = listOf(
                Waypoint(0.00, 0.00, 940.0, "Appalachian Gap"),
                Waypoint(0.22, -0.10, 680.0, "Burnt Spruce Ridge"),
                Waypoint(0.46, -0.22, 410.0, "Hidden Orchard Bunker"),
                Waypoint(0.70, -0.14, 290.0, "Viaduct Overlook"),
                Waypoint(0.88, -0.28, 120.0, "Abandoned Train Yard"),
                Waypoint(1.00, -0.34, 15.0, "Grey Coastline")
            ),
            loreDrops = listOf(
                LoreDrop(
                    id = "road_gap",
                    title = "Crossing the High Divide",
                    category = "Behind the Scenes",
                    quote = "Grey snow drifts silently across the cracked mountain asphalt.",
                    content = "Cormac McCarthy conceived the story while looking out over El Paso at night with his young son, imagining the eastern ridges decades ahead.",
                    waypointIndex = 0
                ),
                LoreDrop(
                    id = "road_bunker",
                    title = "Lantern in the Storm Cellar",
                    category = "Trivia",
                    quote = "Beneath wet pine needles lies a forgotten latch of quiet hope.",
                    content = "Filmed in abandoned coal-country regions of western Pennsylvania and Oregon's Mount St. Helens ash fields to achieve authentic monochrome grit.",
                    waypointIndex = 2
                ),
                LoreDrop(
                    id = "road_coast",
                    title = "Carrying the Fire to the Sea",
                    category = "Lore",
                    quote = "Pale surf breaks softly against driftwood and iron hulls.",
                    content = "The coastal highway marks the end of the father's long push south, leaving the boy to carry forward their shared compass.",
                    waypointIndex = 5
                )
            )
        ),
        FictionalTrek(
            id = "dune_paul_sietch",
            title = "Deep Desert Sietch Tabr Run",
            universe = "Dune",
            characterName = "Paul Atreides",
            characterRole = "Desert Muad'Dib",
            description = "Stride off-rhythm across wind-carved erg dunes and basalt shield walls toward hidden Fremen cisterns in the deep desert.",
            difficulty = TrekDifficulty.LEGENDARY,
            estimatedKm = 10.8,
            elevationGainM = 760,
            accentColorHex = "#F0B65E",
            waypoints = listOf(
                Waypoint(0.00, 0.00, 310.0, "Shield Wall Notch"),
                Waypoint(0.25, 0.18, 240.0, "Drum-Sand Basin"),
                Waypoint(0.55, 0.08, 490.0, "Basalt Outcrop"),
                Waypoint(0.80, 0.28, 680.0, "Wind-Trap Crest"),
                Waypoint(1.00, 0.16, 540.0, "Sietch Tabr Cavern")
            ),
            loreDrops = listOf(
                LoreDrop(
                    id = "dune_sandwalk",
                    title = "Walk Without Rhythm",
                    category = "Lore",
                    quote = "Irregular footsteps blend into shifting wind across the open basin.",
                    content = "Frank Herbert studied coastal sand-dune stabilization projects in Florence, Oregon, inspiring Arrakis's ecological engineering.",
                    waypointIndex = 1
                ),
                LoreDrop(
                    id = "dune_sietch",
                    title = "Water-Sealed Rock Sanctuary",
                    category = "Behind the Scenes",
                    quote = "Cool shadows and hidden cisterns wait behind camouflaged basalt doors.",
                    content = "Wadi Rum in Jordan served as the sweeping desert expanse for the Shield Wall and deep erg crossings.",
                    waypointIndex = 4
                )
            )
        )
    )

    val allUniverses: List<String> = treks.map { it.universe }.distinct()

    val allCharacters: List<String> = treks.map { it.characterName }.distinct()

    fun findById(id: String): FictionalTrek? = treks.firstOrNull { it.id == id }
}
