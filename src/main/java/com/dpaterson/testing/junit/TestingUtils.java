package com.dpaterson.testing.junit;
import org.junit.Test;

import java.lang.reflect.Method;

/**
 * Created by davidpaterson on 14/12/2016.
 */
public class TestingUtils {
    public static boolean isJUnit4Test(Method method){
        return method.getAnnotation(Test.class) != null;
    }
}
