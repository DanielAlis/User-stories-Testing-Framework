package solution;

import org.junit.ComparisonFailure;
import provided.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StoryTesterImpl implements StoryTester {

    private Map<Field, Object> backup;
    private StoryTestExceptionImpl testFailedExc = null;
    private int failCount = 0;


//region Backup
// -----------------------------------------------------------------------------------------------------------------------

    private void createBackup(Class<?> testClass, Object testObj)
            throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        backup = new HashMap<>();
        Field[] fields = testClass.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            if (field.get(testObj) instanceof Cloneable) {
                // is Cloneable
                Method cloneMethod = field.getType().getDeclaredMethod("clone");
                cloneMethod.setAccessible(true);
                backup.put(field, cloneMethod.invoke(field.get(testObj)));
            } else {
                try {
                    // has a copy constructor
                    Constructor cpyCtr = field.getType().getDeclaredConstructor(field.getType());
                    cpyCtr.setAccessible(true);
                    backup.put(field, cpyCtr.newInstance(field.get(testObj)));
                } catch (NoSuchMethodException | InvocationTargetException e) {
                    // not cloneable and no copy constructor.
                    backup.put(field, field.get(testObj));
                }
            }
        }
    }

    private void restoreBackup(Class<?> testClass, Object testObj)
            throws IllegalAccessException {
        Field[] fields = testClass.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            field.set(testObj, backup.get(field));

        }
    }

// -----------------------------------------------------------------------------------------------------------------------
//endregion

    private Object createNewObject(Class<?> testClass)
            throws Exception {
        if (testClass == null) {
            return null;
        }
        Object new_object;
        try {
            Constructor ctr = testClass.getDeclaredConstructor();
            ctr.setAccessible(true);
            new_object = ctr.newInstance();
        } catch (NoSuchMethodException e) {
            Object parent = createNewObject(testClass.getEnclosingClass());
            Constructor ctr = testClass.getDeclaredConstructor(testClass.getEnclosingClass());
            ctr.setAccessible(true);
            new_object = ctr.newInstance(parent);
        }
        return new_object;
    }

    private ArrayList<Sentence> parseStory(String story) {
        ArrayList<Sentence> sentencesList = new ArrayList<Sentence>();

        if (story == null || story.isEmpty()) {
            return sentencesList;
        }

        String[] lines = story.split("\n");
        for (String line : lines) {
            Pattern r = Pattern.compile("([^ ]+) (.+) ([^ ]+)");
            Matcher m = r.matcher(line);
            if (m.find()) {
                String firstWord = m.group(1);
                String sentence = m.group(2);
                String param = m.group(3);
                switch (firstWord) {
                    case "Given":
                        sentencesList.add(new Sentence(SentenceType.GIVEN, sentence, param, line));
                        break;
                    case "When":
                        sentencesList.add(new Sentence(SentenceType.WHEN, sentence, param, line));
                        break;
                    case "Then":
                        sentencesList.add(new Sentence(SentenceType.THEN, sentence, param, line));
                        break;
                }
            } else {
                // TODO maybe other exception?
                throw new IllegalArgumentException();
            }
        }
        return sentencesList;
    }

    private Method findMethod(Sentence sentence, Class<?> testClass)
            throws WordNotFoundException {
        Class<?> currClass = testClass;
        while (currClass != null) {
            Method[] methods = currClass.getDeclaredMethods();
            for (Method method : methods) {
                switch (sentence.X) {
                    case GIVEN:
                        if (method.isAnnotationPresent(Given.class)) {
                            Given ann = method.getAnnotation(Given.class);
                            if (ann.value().substring(0, (ann.value().lastIndexOf(" "))).equals(sentence.Y)) {
                                return method;
                            }
                        }
                        break;
                    case WHEN:
                        if (method.isAnnotationPresent(When.class)) {
                            When ann = method.getAnnotation(When.class);
                            if (ann.value().substring(0, (ann.value().lastIndexOf(" "))).equals(sentence.Y)) {
                                return method;
                            }
                        }
                        break;
                    case THEN:
                        if (method.isAnnotationPresent(Then.class)) {
                            Then ann = method.getAnnotation(Then.class);
                            if (ann.value().substring(0, (ann.value().lastIndexOf(" "))).equals(sentence.Y)) {
                                return method;
                            }

                        }
                        break;
                }
            }
            currClass = currClass.getSuperclass();
        }
        // Method not found
        throw new WordNotFoundException();
    }

    private void invokeMethodWithParam(Method givenMethod, Object testObj, String param)
            throws InvocationTargetException, IllegalAccessException {
        givenMethod.setAccessible(true);
        if (givenMethod.getParameterTypes()[0].equals(Integer.class)) {
            givenMethod.invoke(testObj, Integer.parseInt(param));
        } else {
            givenMethod.invoke(testObj, param);
        }
    }

    /**
     * Runs a given story on an instance of a given class, or an instances of its
     * ancestors. before running the story use must create an instance of the given
     * class.
     *
     * @param story     contains the text of the story to test, the string is
     *                  divided to line using '\n'. each word in a line is separated by space
     *                  (' ').
     * @param testClass the test class which the story should be run on.
     */
    public void testOnInheritanceTree(String story, Class<?> testClass)
            throws Exception {
        testFailedExc = null;
        if (story == null || testClass == null) {
            throw new IllegalArgumentException();
        }
        ArrayList<Sentence> parsedStory = parseStory(story);
        Object testObj = createNewObject(testClass);


        // FIND GIVEN Method
        Method givenMethod;
        try {
            givenMethod = findMethod(parsedStory.get(0), testClass);
            invokeMethodWithParam(givenMethod, testObj, parsedStory.get(0).Z);
        } catch (WordNotFoundException e) {
            throw new GivenNotFoundException();
        }
        // Invoking WHEN Statements
        boolean backupNeeded = true;
        for (Sentence sentence : parsedStory.subList(1, parsedStory.size())) {
            if (sentence.X.equals(SentenceType.WHEN)) {
                // Backup before When Series starts
                if (backupNeeded) {
                    createBackup(testClass, testObj);
                    backupNeeded = false;
                }
                try {
                    Method m = findMethod(sentence, testClass);
                    invokeMethodWithParam(m, testObj, sentence.Z);
                } catch (WordNotFoundException e) {
                    throw new WhenNotFoundException();
                }
            }
            // Invoking THEN Statements
            else if (sentence.X.equals(SentenceType.THEN)) {
                backupNeeded = true;
                try {
                    Method m = findMethod(sentence, testClass);
                    invokeMethodWithParam(m, testObj, sentence.Z);
                } catch (InvocationTargetException e1) {
                    ComparisonFailure e = (ComparisonFailure) e1.getCause();
                    restoreBackup(testClass, testObj);
                    if (testFailedExc == null) {
                        testFailedExc = new StoryTestExceptionImpl(sentence, e.getExpected(), e.getActual());
                    } else {
                        testFailedExc.incFailCount();
                    }
                } catch (WordNotFoundException e) {
                    throw new ThenNotFoundException();
                }
            }
        }
        if (testFailedExc != null) {
            throw testFailedExc;
        }
    }

    /**
     * Runs a given story on an instance of a given class, or an instances of its
     * ancestors, or its nested class (and their ancestors) as described by the
     * the assignment document. before running the story use must create an instance
     * of the given correct class to run story on.
     *
     * @param story     contains the text of the story to test, the string is
     *                  divided to line using '\n'. each word in a line is separated by space
     *                  (' ').
     * @param testClass the test class which the story should be run on.
     */
    public void testOnNestedClasses(String story, Class<?> testClass)
            throws Exception {
        try {
            testOnInheritanceTree(story, testClass);

        } catch (GivenNotFoundException e) {
            Class<?>[] nestedClasses = testClass.getDeclaredClasses();
            boolean foundGiven = false;
            for (Class<?> nested : nestedClasses) {
                try {
                    testOnNestedClasses(story, nested);
                    foundGiven = true;
                    break;
                } catch (GivenNotFoundException e1) {
                    continue;
                }
            }
            if (!foundGiven) {
                throw new GivenNotFoundException();
            }

        }

    }
}
