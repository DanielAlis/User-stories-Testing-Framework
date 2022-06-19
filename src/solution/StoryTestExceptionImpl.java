package solution;

import provided.StoryTestException;
// This is where the magic happens
public class StoryTestExceptionImpl extends StoryTestException {
    private int failCount;
    private String firstExpected;
    private String firstActual;
    Sentence firstFailedSentence;

    public StoryTestExceptionImpl(Sentence sentence, String expected ,String actual)
    {
        failCount = 1;
        firstFailedSentence = sentence;
        firstExpected = expected;
        firstActual = actual;
    }
    public String getSentance(){
        // TODO fix Sentence toString()
        return firstFailedSentence.toString();
    }

    /**
     * Returns a string representing the expected value from the story
     * of the first Then sentence that failed.
     */
    public String getStoryExpected(){
        return firstExpected;
    }

    /**
     * Returns a string representing the actual value.
     * of the first Then sentence that failed.
     */
    public String getTestResult(){
        return firstActual;
    }

    /**
     * Returns an int representing the number of Then sentences that failed.
     */
    public int getNumFail(){
        return failCount;
    }

    public void incFailCount() {
        failCount++;
    }
}
