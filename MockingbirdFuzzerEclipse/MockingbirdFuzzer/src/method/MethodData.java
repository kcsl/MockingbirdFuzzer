package method;

import java.time.Duration;

/**
 * @author Derrick Lockwood
 * @created 6/7/18.
 */
public class MethodData {
    private final Class<?> declaringClass;
    private final String methodName;
    private final Class<?> returnType;
    private final Class<?>[] parameterTypes;
    private final Object mockObject;
    private final Object[] parameters;

    private transient Object returnValue;
    private transient Exception returnException;
    private transient Duration duration;
    private transient long deltaHeapMemory;

    MethodData(Object mockObject,
               Object[] parameters,
               Class<?> declaringClass,
               String methodName,
               Class<?> returnType,
               Class<?>... parameterTypes) {
        this.mockObject = mockObject;
        this.parameters = parameters;
        this.methodName = methodName;
        this.declaringClass = declaringClass;
        this.returnType = returnType;
        this.parameterTypes = parameterTypes;
    }

    void setOutput(Object returnValue, Exception returnException, Duration duration, long deltaHeapMemory) {
        this.returnValue = returnValue;
        this.returnException = returnException;
        this.duration = duration;
        this.deltaHeapMemory = deltaHeapMemory;
    }

    public Object getMockObject() {
        return mockObject;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public Class<?> getDeclaringClass() {
        return declaringClass;
    }

    public String getMethodName() {
        return methodName;
    }

    public Class<?> getReturnType() {
        return returnType;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    public Object getReturnValue() {
        return returnValue;
    }

    public Exception getReturnException() {
        return returnException;
    }

    public Duration getDuration() {
        return duration;
    }

    public long getDeltaHeapMemory() {
        return deltaHeapMemory;
    }

    @Override
    public String toString() {
        return "Method: " + methodName + " Duration: " + duration.toString() + " Memory: " + deltaHeapMemory + " Result: " + (returnValue == null ? (returnException == null ? "null" : returnException.toString()) : returnValue.toString());
    }
}
