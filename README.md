# FXTrackr

FXTrackr is a backend application for currency exchange and conversion, built with Spring Boot.

It supports real-time exchange rate retrieval, currency conversion logging, and basic transaction history.

## Run with Docker

1. Make sure Docker is installed and the daemon is running.
2. In your shell, set your Fixer.io API key:
   ```bash
   export FIXER_API_KEY={YOUR_FIXER_API_KEY}
3. From the project root (where Dockerfile and docker-compose.yml live), build and start the service:

   ```bash
   docker-compose up --build

4. Once it’s up, you can explore:

   * Swagger UI: http://localhost:8080/swagger-ui.html

   * H2 Console: http://localhost:8080/h2-console
     * JDBC URL: jdbc:h2:mem:exchange_db 
     * User: sa (no password)

5. By default, tests are skipped during Docker build (`-DskipTests`).  
   If you’d like the container build to fail on test failures, edit your `Dockerfile`:

    ```diff
   -    RUN mvn clean package -DskipTests
   +    RUN mvn clean package

## API Endpoints

### 1. Get Exchange Rate
**GET** `/api/exchange-rate`

- **Request**  
  Query parameters:
    - `from`: 3-letter source currency code (e.g. USD)
    - `to`:   3-letter target currency code (e.g. EUR)

- **Response (200 OK)**  
  JSON with:
    - `exchangeRate`: current rate (number)

- **Errors**
    - `400 Bad Request` – INVALID_PARAMETER_FORMAT – if from or to is missing or not three letters
    - `400 Bad Request` – INVALID_CURRENCY – if the code is syntactically valid but not supported
    - `500 Internal Server Error` – INTERNAL_ERROR – for any unexpected server failure
    - `502 Bad Gateway` – EXTERNAL_API_ERROR – if the external FX service fails

---

### 2. Convert Single Amount
**POST** `/api/convert`

- **Request**  
  JSON body with:
    - `amount`: positive number (source amount)
    - `from`:   3-letter source currency code
    - `to`:     3-letter target currency code

- **Response (200 OK)**  
  JSON with:
    - `transactionId`: unique ID of this conversion
    - `convertedAmount`: calculated target amount (number)

- **Errors**
    - `400 Bad Request` – INVALID_PARAMETER_FORMAT – JSON malformation or missing fields
    - `400 Bad Request` – INVALID_AMOUNT – amount<=0 or too many digits
    - `400 Bad Request` – INVALID_CURRENCY – unsupported currency code
    - `500 Internal Server Error` – INTERNAL_ERROR – unexpected error
    - `502 Bad Gateway` – EXTERNAL_API_ERROR – FX service call failed

---

### 3. Search Conversion History
**POST** `/api/conversions/search`

- **Request**  
  A JSON body containing **at least one** of the following properties:
    - `transactionId`: UUID of a previous conversion
    - `date`: filter by date in `YYYY-MM-DD` format

- **Response (200 OK)**  
  A paginated result with:
    - `content`: list of conversion records, each including
        - `transactionId`
        - `sourceCurrency`
        - `targetCurrency`
        - `sourceAmount`
        - `convertedAmount`
        - `exchangeRate`
        - `timestamp` (ISO-8601)
    - `totalElements`: total number of records
    - `pageable`: pagination metadata

- **Errors**
    - **400 Bad Request**
        - `400 Bad Request` – MISSING_FILTER – if both transactionId and date are absent
        - `400 Bad Request` – INVALID_PARAMETER_FORMAT – if date isn’t YYYY-MM-DD
        - `500 Internal Server Error` – INTERNAL_ERROR – unexpected error

---

### 4. Bulk CSV Conversion
**POST** `/api/convert/bulk`

- **Request**  
  `multipart/form-data` with one field:
    - `file`: CSV file (headers must be `amount,from,to`)

- **Response (200 OK)**  
  JSON array of per-row results, each containing:
    - `line`: row number (integer)
    - `transactionId`: ID if successful, or `null`
    - `convertedAmount`: number if successful, or `null`
    - `code`: status code (`SUCCESS`, `INVALID_AMOUNT`, `EXTERNAL_API_ERROR`, etc.)
    - `message`: human-readable detail

- **Errors**
    - `400 Bad Request` – INVALID_CSV_HEADER – if header row is missing or wrong columns
    - `500 Internal Server Error` – BULK_PROCESSING_ERROR – on unexpected processing failures