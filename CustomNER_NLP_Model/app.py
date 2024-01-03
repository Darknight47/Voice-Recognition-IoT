from flask import Flask, request, jsonify
import spacy

app = Flask(__name__)

# Load your trained model
# nlp = spacy.load("/Users/viggorunsten/Voice-Recognition-IoT/CustomNER_NLP_Model/voicecommander_custom_ner_nlp_model")
nlp = spacy.load("/usr/src/app/voicecommander_custom_ner_nlp_model")


@app.route('/ner', methods=['POST'])
def get_entities():
    data = request.json
    text = data['text']
    doc = nlp(text)
    entities = [{'text': ent.text, 'label': ent.label_} for ent in doc.ents]
    return jsonify(entities)

if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0')
