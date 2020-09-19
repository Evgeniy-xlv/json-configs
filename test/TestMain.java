public class TestMain {

    public static void main(String[] args) {
        TestConfig testConfig = new TestConfig();
        testConfig.getStringList().add("testString1");
        testConfig.getStringList().add("testString2");
        testConfig.getStringList().add("testString3");
        testConfig.getMapTest().put("1", "aaa");
        testConfig.getMapTest().put("2", "bbb");
        testConfig.getMapTest().put("3", "ccc");
        System.out.println(testConfig);
        testConfig.save();
        testConfig.load();
        System.out.println(testConfig);
    }
}
