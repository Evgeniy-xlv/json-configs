import ru.xlv.config.Configurable;
import ru.xlv.config.IConfigGson;
import ru.xlv.config.Version;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configurable
@Version("1.0.0")
public class TestConfig implements IConfigGson {

    private String testString = "testStringValue";
    private int testInt = 123;
    private double testDouble = 123.3D;
    private List<String> stringList = new ArrayList<>();
    private Map<String, String> mapTest = new HashMap<>();

    @Override
    public File getConfigFile() {
        return new File("test_config.json");
    }

    public void setTestString(String testString) {
        this.testString = testString;
    }

    public void setTestInt(int testInt) {
        this.testInt = testInt;
    }

    public void setTestDouble(double testDouble) {
        this.testDouble = testDouble;
    }

    public void setStringList(List<String> stringList) {
        this.stringList = stringList;
    }

    public void setMapTest(Map<String, String> mapTest) {
        this.mapTest = mapTest;
    }

    public String getTestString() {
        return testString;
    }

    public int getTestInt() {
        return testInt;
    }

    public double getTestDouble() {
        return testDouble;
    }

    public List<String> getStringList() {
        return stringList;
    }

    public Map<String, String> getMapTest() {
        return mapTest;
    }

    @Override
    public String toString() {
        return "TestConfig{" +
                "testString='" + testString + '\'' +
                ", testInt=" + testInt +
                ", testDouble=" + testDouble +
                ", stringList=" + stringList +
                ", mapTest=" + mapTest +
                '}';
    }
}
