package se.edugrade.java25.restaurant.api;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;
import se.edugrade.java25.restaurant.dto.BookingPageResponse;
import se.edugrade.java25.restaurant.dto.BookingRequest;
import se.edugrade.java25.restaurant.dto.BookingResponse;

public interface BookingApi {

        @Operation(summary = "List bookings", description = "Retrieves a paginated list of bookings using offset-based pagination.")
        @ApiResponse(responseCode = "200", description = "Bookings retrieved successfully")
        @ApiResponse(responseCode = "400", description = "Invalid pagination parameters",
                content = @Content(
                        mediaType = "application/problem+json",
                        schema = @Schema(implementation = ProblemDetail.class)))

        BookingPageResponse findAll(
                @ParameterObject Pageable pageable);

        @Operation(summary = "Get booking by ID", description = "Retrieves a booking by its unique identifier. Supports conditional requests using ETag and If-None-Match.")
        @ApiResponse(responseCode = "200", description = "Booking retrieved successfully", headers = @Header(
                        name = "ETag",
                        description = "Version identifier for the booking"))

        @ApiResponse(responseCode = "304", description = "Booking has not changed since the supplied ETag",
                content = @Content)

        @ApiResponse(responseCode = "404", description = "Booking not found",
                content = @Content(
                        mediaType = "application/problem+json",
                        schema = @Schema(implementation = ProblemDetail.class)))

        ResponseEntity<EntityModel<BookingResponse>> findById(
                @Parameter(description = "Unique identifier of the booking", example = "1")
                Long id, @Parameter(hidden = true) WebRequest req);

        @Operation(summary = "Create a booking", description = "Creates a new booking with REQUESTED status and returns its details.")
        @ApiResponse(responseCode = "201", description = "Booking created successfully",
                headers = @Header(
                        name = "Location",
                        description = "URL of the newly created booking"))
        @ApiResponse(responseCode = "400", description = "Invalid booking data",
                content = @Content(
                        mediaType = "application/problem+json",
                        schema = @Schema(implementation = ProblemDetail.class)))
        @ApiResponse(responseCode = "404", description = "Restaurant not found",
                content = @Content(
                        mediaType = "application/problem+json",
                        schema = @Schema(implementation = ProblemDetail.class)
                )
        )
        ResponseEntity<EntityModel<BookingResponse>> create(
                BookingRequest req
        );

        @Operation(
                summary = "Confirm a booking",
                description = "Changes the booking status from REQUESTED to CONFIRMED."
        )
        @ApiResponse(
                responseCode = "200",
                description = "Booking confirmed successfully"
        )
        @ApiResponse(
                responseCode = "404",
                description = "Booking not found",
                content = @Content(
                        mediaType = "application/problem+json",
                        schema = @Schema(implementation = ProblemDetail.class)
                )
        )
        @ApiResponse(
                responseCode = "409",
                description = "Booking cannot be confirmed in its current state",
                content = @Content(
                        mediaType = "application/problem+json",
                        schema = @Schema(implementation = ProblemDetail.class)
                )
        )
        EntityModel<BookingResponse> confirm(
                @Parameter(
                        description = "Unique identifier of the booking",
                        example = "1"
                )
                Long id
        );

        @Operation(
                summary = "Seat a booking",
                description = "Changes the booking status from CONFIRMED to SEATED."
        )
        @ApiResponse(
                responseCode = "200",
                description = "Booking seated successfully"
        )
        @ApiResponse(
                responseCode = "404",
                description = "Booking not found",
                content = @Content(
                        mediaType = "application/problem+json",
                        schema = @Schema(implementation = ProblemDetail.class)
                )
        )
        @ApiResponse(
                responseCode = "409",
                description = "Booking cannot be seated in its current state",
                content = @Content(
                        mediaType = "application/problem+json",
                        schema = @Schema(implementation = ProblemDetail.class)
                )
        )
        EntityModel<BookingResponse> seat(
                @Parameter(
                        description = "Unique identifier of the booking",
                        example = "1"
                )
                Long id
        );

        @Operation(
                summary = "Cancel a booking",
                description = "Changes the booking status from REQUESTED or CONFIRMED to CANCELLED."
        )
        @ApiResponse(
                responseCode = "200",
                description = "Booking cancelled successfully"
        )
        @ApiResponse(
                responseCode = "404",
                description = "Booking not found",
                content = @Content(
                        mediaType = "application/problem+json",
                        schema = @Schema(implementation = ProblemDetail.class)
                )
        )
        @ApiResponse(
                responseCode = "409",
                description = "Booking cannot be cancelled in its current state",
                content = @Content(
                        mediaType = "application/problem+json",
                        schema = @Schema(implementation = ProblemDetail.class)
                )
        )
        EntityModel<BookingResponse> cancel(
                @Parameter(
                        description = "Unique identifier of the booking",
                        example = "1"
                )
                Long id
        );
}


