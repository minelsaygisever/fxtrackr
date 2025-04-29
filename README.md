# FXTrackr

FXTrackr is a backend application for currency exchange and conversion, built with Spring Boot.

It supports real-time exchange rate retrieval, currency conversion logging, and basic transaction history.

## Configuration

1. Copy `application.properties.example` to `application.properties`.
2. Provide your Fixer API key by:
    - Replacing `${FIXER_API_KEY}` in `application.properties` with the actual key

All other settings (H2, JPA, Swagger, logging) work out of the box.