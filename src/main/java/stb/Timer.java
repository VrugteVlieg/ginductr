package stb;

import java.util.HashMap;

public class Timer {
    private long startTime;

    private HashMap<String, Long> startTimes = new HashMap<>();
    public Timer() {
        startTime = 0;
    }



    public void startClock() {
        startTime = System.nanoTime();
    }

    public void startClock(String key) {
        // System.err.println("Starting clock for " + key);
        if(startTimes.containsKey(key)) {
            startTimes.replace(key, System.nanoTime());
        } else {
            startTimes.put(key, System.nanoTime());
            // System.err.println(startTimes.keySet());
        }
    }



    /**
     * @return number of seconds elapsed since last call to startClock()
     */
    public double elapsedTime() {
        return (System.nanoTime() - startTime) / Math.pow(10, 9);
    }

    public double elapsedTime(String key) {
//        System.err.println(startTimes.keySet());
        return (System.nanoTime() - startTimes.get(key)) / Math.pow(10, 9);
    }

    public double split() {
        double out = elapsedTime();
        startClock();
        return out;
    }

    public double split(String key) {
        double out = elapsedTime(key);
        startClock(key);
        return out;
    }

    public void removeKey(String key) {
        startTimes.remove(key);
    }

    public double stop(String key) {
        double out = elapsedTime(key);
        startTimes.remove(key);
        return out;
    }



	public void clear() {
        startTimes.clear();
        startTime = 0;
	}
}
