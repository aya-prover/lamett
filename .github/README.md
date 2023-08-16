[![test](https://github.com/aya-prover/lamett/actions/workflows/gradle-check.yml/badge.svg)](https://github.com/aya-prover/lamett/actions/workflows/gradle-check.yml)

The project `lamett` is a parody of cooltt, but instead of case expressions, we have Agda style pattern matching (i.e. clause-based).
This part of the code is copied from [ice1000/anqur](https://github.com/ic1000/anqur).
We attempt to experiment with the following features, before adding them to Aya:

- Diagonal cofibrations -- an extensional equality on variables
- Automatic coercive subtyping during elaboration
- Generalized extension types for controlling unfolding, class extensions, and cubical
- Tarski-style universe which decodes both fibrant and exo-types into "types"
- How does phase distinction look like in the implementation of cubical type theory

We've finished the first and the last feature, and is currently working on the rest of the list.
These might be done in an incomplete way because essentially the purpose of this project is to experiment before adding them to Aya, and if we know how to do it in Aya, we may not do it here.
