# Task Decomposition Model

For each workflow, the application writes requirement and task artifacts under `work/generated/`. The derived sequence is requirement understanding, design, implementation, validation, documentation, and release approval. Dependencies are enforced by the workflow graph; independent ready stages are candidates for later parallel execution.
