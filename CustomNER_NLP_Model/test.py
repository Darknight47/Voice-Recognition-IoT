import spacy

# Load the trained model
nlp = spacy.load("/Users/viggorunsten/Voice-Recognition-IoT/CustomNER_NLP_Model/voicecommander_custom_ner_nlp_model")
# Create a document object by passing the input text to the model
doc = nlp('Turn on the lamp in aulan')

# Iterate over the entities identified by the model
for ent in doc.ents:
    print(ent.text, ent.label_)
