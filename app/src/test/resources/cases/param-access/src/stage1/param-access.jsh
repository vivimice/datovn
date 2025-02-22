
int params = Integer.parseInt(System.getenv("DATOVN_PARAMS_COUNT"))
System.out.println("Params count: " + params)
for (int i = 0; i < params; i++) {
    System.out.println("Param #" + i + ": " + System.getenv("DATOVN_PARAM_VALUE_" + i));
}

/exit