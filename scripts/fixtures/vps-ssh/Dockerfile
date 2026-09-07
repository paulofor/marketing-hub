FROM debian:bookworm-slim
RUN apt-get update \
    && apt-get install -y --no-install-recommends openssh-client openssh-server rsync \
    && rm -rf /var/lib/apt/lists/* /etc/ssh/ssh_host_*
WORKDIR /app
COPY configure-vps-ssh-fallback.sh ssh-keyscan-with-retry.sh test.sh ./
# Reproduz o modo versionado do helper em um checkout limpo.
RUN chmod 644 /app/ssh-keyscan-with-retry.sh
ENTRYPOINT ["bash", "/app/test.sh"]
