package solution;


public class Sentence {
//    public Class<? extends Annotation> type;
    public SentenceType X;
    public String Y;
    public String Z;
    public String fullSentence;

    Sentence(SentenceType t, String value, String p, String line) {
        X = t;
        Y = value;
        Z = p;
        fullSentence = line;
    }
    @Override
    public String toString() {
        return fullSentence;
    }
}
