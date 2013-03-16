
public class SomeClass {
	public String first;
	public String second;
	
	private String hidden;
	private NestedClass nested;
	
	public SomeClass()
	{
		nested = new NestedClass();
	}

	public String getHidden() {
		return "prefix" + hidden;
	}

	public void setHidden(String hidden) {
		this.hidden = hidden;
	}
	
	public class NestedClass
	{
		public String nested = "defaultNestedValue";
	}
}
