
# Container

You run in a Docker container. The host's current directory is mounted at
/work; nothing else from the host is visible. Tools come from the
image, so don't install packages with apt, pip or npm -g: they would not
persist. As root in a container, Chromium needs `--no-sandbox`.
