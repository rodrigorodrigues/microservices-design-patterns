# Build stage
FROM rust:1.93.0-slim as builder

ARG PORT

WORKDIR /app

# Install dependencies
RUN apt-get update && apt-get install -y \
    pkg-config \
    libssl-dev \
    && rm -rf /var/lib/apt/lists/*

# Copy manifests
COPY Cargo.toml Cargo.lock ./

# Copy source code
COPY src ./src

# Build application
RUN cargo build --release

# Runtime stage
FROM gcr.io/distroless/cc-debian12

ARG PORT
ENV LISTEN_PORT=$PORT

WORKDIR /app

# Copy the binary from builder
COPY --from=builder /app/target/release/rust-service /app/rust-service

# Copy .env file if exists
#COPY .env* /app/ 2>/dev/null || true

EXPOSE $PORT

CMD ["/app/rust-service"]
