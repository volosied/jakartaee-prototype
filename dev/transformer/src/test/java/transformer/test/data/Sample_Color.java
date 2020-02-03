package transformer.test.data;

public interface Sample_Color {
	public @interface Color {
		Value value() default Value.RED;
		public enum Value { RED, BLUE, YELLOW }
	}
}
