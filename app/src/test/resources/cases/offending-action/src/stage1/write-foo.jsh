Files.write(
    Path.of(System.getenv("DATOVN_ACTIONS_OUTPUT_FILE")), 
    """
    ---
    type: fileAccess
    path: foo.txt
    mode: write
    """.getBytes()
);
/exit