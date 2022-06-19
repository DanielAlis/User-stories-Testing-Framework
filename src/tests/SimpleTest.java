package tests;

import solution.StoryTesterImpl;

public class SimpleTest {

    static String goodStory = "Given a Dog of age 6\n"
            + "When the dog is not taken out for a walk, and the number of hours is 5\n"
            + "Then the house condition is clean";

    static String badStory = "Given a Dog of age 6\n"
            + "When the dog is not taken out for a walk, and the number of hours is 5\n"
            + "Then the house condition is smelly";

    static String derivedStory = "Given a Dog of age 6\n"
            + "When the dog is not taken out for a walk, and the number of hours is 15\n"
            + "When the house is cleaned, and the number of hours is 11\n"
            + "Then the house condition is clean";

    static String nestedStory = "Given a Dog that his age is 6\n"
            + "When the dog is not taken out for a walk, and the number of hours is 5\n"
            + "Then the house condition is clean";
    public static void main(String args[])
    {
        StoryTesterImpl tester = new StoryTesterImpl();
//        tester.ParseStory(derivedStory);
    }
}
