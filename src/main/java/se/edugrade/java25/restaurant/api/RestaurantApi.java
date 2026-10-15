package se.edugrade.java25.restaurant.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import se.edugrade.java25.restaurant.dto.RestaurantPageResponse;
import se.edugrade.java25.restaurant.dto.RestaurantRequest;
import se.edugrade.java25.restaurant.dto.RestaurantResponse;

public interface RestaurantApi {

    @Operation(summary = "List restaurants", description = "Retrieves a paginated list of restaurants using offset-based pagination.")
    @ApiResponse(responseCode = "200", description = "Restaurants retrieved successfully")
    @ApiResponse(responseCode = "400", description = "Invalid pagination parameters",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "Authentication is required")
    @ApiResponse(responseCode = "403", description = "Insufficient permissions to read restaurants")

    RestaurantPageResponse findAll(@Parameter(description = "Pagination and sorting parameters") Pageable pageable);

    @Operation(
            summary = "Get restaurant by ID", description = "Retrieves a restaurant by its unique identifier.")
    @ApiResponse(responseCode = "200", description = "Restaurant retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Authentication is required")
    @ApiResponse(responseCode = "403", description = "Insufficient permissions to read restaurants")
    @ApiResponse(responseCode = "404", description = "Restaurant not found",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))

    RestaurantResponse findById(@Parameter(description = "Unique identifier of the restaurant", example = "1") Long id);

    @Operation(summary = "Create a restaurant", description = "Creates a new restaurant and returns its details.")
    @ApiResponse(responseCode = "201", description = "Restaurant created successfully. The Location header contains the URL of the new restaurant.")
    @ApiResponse(responseCode = "400", description = "Invalid restaurant data",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "Authentication is required")
    @ApiResponse(responseCode = "403", description = "Insufficient permissions to create restaurants")

    ResponseEntity<RestaurantResponse> create(RestaurantRequest req);
    @Operation(summary = "Update a restaurant", description = "Updates an existing restaurant and returns its updated details.")

    @ApiResponse(responseCode = "200", description = "Restaurant updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid restaurant data",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))

    @ApiResponse(responseCode = "401", description = "Authentication is required")
    @ApiResponse(responseCode = "403", description = "Insufficient permissions to update restaurants")
    @ApiResponse(responseCode = "404", description = "Restaurant not found",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))

    RestaurantResponse update(@Parameter(description = "Unique identifier of the restaurant to update", example = "1")
            Long id, RestaurantRequest req);
}

