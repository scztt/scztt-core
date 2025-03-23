Praxis {
    classvar intro = #[
        "YOU ATTEMPT TO RECAPTURE YOUR OLDER, MORE",
        "EFFECTIVE PRAXIS",
        "",
        "It involves some combination of",
        ""
    ];
    
    classvar lines = #[
        "lucid dreams,",
        "sea water,",
        "grated wood,",
        "personal artifacts,",
        "broken drafting utensils,",
        "books on metropolitan architecture,",
        "carnivore teeth,",
        "disassembled objects,",
        "psychogeography,",
        "literature from the expanded field,",
        "substance,",
        "dark roasted coffee,",
        "lemon,",
        "honey,",
        "garamond typesetting,",
        "empty space,",
        "anti-accelerationism,",
        "glass mason jars,",
        "Max Ernst etchings,",
        "experimental psychoanalysis,",
        "wolf-men,",
        "model assembling,",
        "books on terminal architecture,",
        "preserved flesh,",
        "deterritorialization,",
        "astral projection,",
        "dilapidated canvas,",
        "fruit-centric rituals,",
        "bone broth,",
        "books about zonetology,",
        "controlled breathing,",
        "wood carving videos,",
        "wikipedia bibliomancy,",
        "crushed cans,",
        "wooden dioramas,",
        "mechanical pencils,",
        "justified text,",
        "dreamed seances,",
        "inappropriate em dashes,",
        "minor acts of violence,",
        "consumption,",
        "mien,",
        "medidation,",
    ];
    
    classvar outro = #["", "but you cannot remember for sure."];
    
    *recapture {
        "\n\n".post;
        (intro ++ lines.scramble[0..5] ++ outro).do {
            |l|
            "    ".post;
            l.postln;
        };
        "\n\n".post;
    }
}
