CS 6322 - Information Retrieval Fall 2018
Author: Parag Pravin Dakle
NetId: pxd160530
Homework 2 Readme

Problem - Index Building

1. How to run the program?
Execute the following command from inside the project directory:
./spimi.sh <path_to_corpus> <stopwords_filepath>

where -
<path_to_corpus>: Path of the directory where the Cranfield collection is present.
<stopwords_filepath>: Path to the file containing stopwords.

2. Sample Execution:
Command: ./spimi.sh Cranfield/ /people/cs/s/sanda/cs6322/resourcesIR/stopwords
Output:

Creating index version 1...
Tokenizing.. Timestamp: 1540855156347
Adding annotator tokenize
TokenizerAnnotator: No tokenizer type provided. Defaulting to PTBTokenizer.
Adding annotator ssplit
edu.stanford.nlp.pipeline.AnnotatorImplementations:
Adding annotator pos
Reading POS tagger model from edu/stanford/nlp/models/pos-tagger/english-left3words/english-left3words-distsim.tagger ... done [0.6 sec].
Adding annotator lemma
Creating uncompressed index.. Timestamp: 1540855169670
Creating compressed index.. Timestamp: 1540855171402
Done. Timestamp: 1540855172187
Creating index version 2...
Tokenizing.. Timestamp: 1540855172187
Creating uncompressed index.. Timestamp: 1540855173402
Creating compressed index.. Timestamp: 1540855174974
Done. Timestamp: 1540855175742
