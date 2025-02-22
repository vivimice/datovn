String content = new String(Files.readAllBytes(Path.of("foo.txt")));
System.out.println(content);

Files.write(
    Path.of(System.getenv("DATOVN_ACTIONS_OUTPUT_FILE")), 
    """
    ---
    type: fileAccess
    path: foo.txt
    mode: read
    """.getBytes(), 
    StandardOpenOption.APPEND
);

/exit