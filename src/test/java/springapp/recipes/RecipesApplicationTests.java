package springapp.recipes;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RecipesApplicationTests {

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void shouldReturnRecipesList() {
        ResponseEntity<String> response = restTemplate
                .withBasicAuth("Peppa_Pig", "Peppa_pwd")
                .getForEntity("/recipes", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        DocumentContext documentContext = JsonPath.parse(response.getBody());

        int recipeCount = documentContext.read("$.length()");
        assertThat(recipeCount).isEqualTo(3);

        JSONArray ids = documentContext.read("$..id");
        assertThat(ids).containsExactlyInAnyOrder(999, 1000, 1001);

        JSONArray titles = documentContext.read("$..title");
        assertThat(titles).containsExactlyInAnyOrder("Makownik", "Paszteciki ze szpinakiem", "Lukier cytrynowy");

        JSONArray urls = documentContext.read("$..url");
        assertThat(urls)
                .containsExactlyInAnyOrder("https://kuchnia-domowa-ani.blogspot.com/2018/12/paszteciki-ze-szpinakiem-i-serem.html",
                        "https://ilovebake.pl/przepis/makowiec-na-kruchym-spodzie", null);

        JSONArray areSweet = documentContext.read("$..isSweet");
        assertThat(areSweet).containsExactlyInAnyOrder(true, true, false);

    }

    @Test
    void shouldReturnAPageOfRecipes() {
        ResponseEntity<String> response = restTemplate.withBasicAuth("Peppa_Pig", "Peppa_pwd")
                .getForEntity("/recipes?page=0&size=1", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        DocumentContext documentContext = JsonPath.parse(response.getBody());
        JSONArray page = documentContext.read("$[*]");
        assertThat(page.size()).isEqualTo(1);
    }

    @Test
    void shouldReturnASortedPageOfRecipes() {
        ResponseEntity<String> response = restTemplate.withBasicAuth("Peppa_Pig", "Peppa_pwd")
                .getForEntity("/recipes?page=0&size=1&sort=title,desc", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        DocumentContext documentContext = JsonPath.parse(response.getBody());
        JSONArray read = documentContext.read("$[*]");
        assertThat(read.size()).isEqualTo(1);

        String title = documentContext.read("$[0].title");
        assertThat(title).isEqualTo("Paszteciki ze szpinakiem");
    }

    @Test
    void shouldReturnASortedPageOfRecipesWithNoParametersAndUseDefaultValues() {
        ResponseEntity<String> response = restTemplate.withBasicAuth("Peppa_Pig", "Peppa_pwd")
                .getForEntity("/recipes", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        DocumentContext documentContext = JsonPath.parse(response.getBody());
        JSONArray page = documentContext.read("$[*]");
        assertThat(page.size()).isEqualTo(3);

        JSONArray titles = documentContext.read("$..title");
        assertThat(titles).containsExactly("Paszteciki ze szpinakiem", "Lukier cytrynowy", "Makownik");
    }

    @Test
    void shouldReturnARecipeWhenDataIsSaved() {
        ResponseEntity<String> response = restTemplate.withBasicAuth("Peppa_Pig", "Peppa_pwd")
                .getForEntity("/recipes/999", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        DocumentContext documentContext = JsonPath.parse(response.getBody());
        Number id = documentContext.read("$.id");
        assertThat(id).isEqualTo(999);

        String title = documentContext.read("$.title");
        assertThat(title).isEqualTo("Makownik");

        String url = documentContext.read("$.url");
        assertThat(url).isEqualTo("https://ilovebake.pl/przepis/makowiec-na-kruchym-spodzie");

        boolean isSweet = documentContext.read("$.isSweet");
        assertThat(isSweet).isTrue();
    }

    @Test
    void shouldNotReturnARecipeWithAnUnknownId() {
        ResponseEntity<String> response = restTemplate.withBasicAuth("Peppa_Pig", "Peppa_pwd")
                .getForEntity("/recipes/5000", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldNotReturnARecipeOwnedByAnotherUser() {
        ResponseEntity<String> response = restTemplate.withBasicAuth("Peppa_Pig", "Peppa_pwd")
                .getForEntity("/recipes/1002", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DirtiesContext
    void shouldCreateANewRecipe() {
        Recipe newRecipe = new Recipe();
        newRecipe.setTitle("title");
        newRecipe.setUrl("url");
        newRecipe.setDescription("description");
        newRecipe.setSweet(false);

        ResponseEntity<Void> createResponse = restTemplate
                .withBasicAuth("Peppa_Pig", "Peppa_pwd")
                .postForEntity("/recipes", newRecipe, Void.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        URI locationOfNewRecipe = createResponse.getHeaders().getLocation();
        ResponseEntity<String> getResponse = restTemplate
                .withBasicAuth("Peppa_Pig", "Peppa_pwd")
                .getForEntity(locationOfNewRecipe, String.class);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        compareRecipeObjectToResponse(getResponse, newRecipe);
    }

    @Test
    @DirtiesContext
    void shouldCreateANewRecipeWithVeryLongDescription() throws IOException {
        Recipe newRecipe = new Recipe();
        newRecipe.setTitle("Peppermint Mocha Cake");
        newRecipe.setUrl("https://www.mycakeschool.com/peppermint-mocha-cake/");
        newRecipe.setSweet(true);

        BufferedReader br = new BufferedReader(new FileReader("src/test/resources/springapp/recipes/very_long_description.txt"));
        try {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
            newRecipe.setDescription(sb.toString());
        } finally {
            br.close();
        }

        ResponseEntity<Void> createResponse = restTemplate
                .withBasicAuth("Peppa_Pig", "Peppa_pwd")
                .postForEntity("/recipes", newRecipe, Void.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        URI locationOfNewRecipe = createResponse.getHeaders().getLocation();
        ResponseEntity<String> getResponse = restTemplate
                .withBasicAuth("Peppa_Pig", "Peppa_pwd")
                .getForEntity(locationOfNewRecipe, String.class);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        compareRecipeObjectToResponse(getResponse, newRecipe);
    }

    @Test
    @DirtiesContext
    void shouldDeleteARecipe() {
        ResponseEntity<Void> response = restTemplate
                .withBasicAuth("Peppa_Pig", "Peppa_pwd")
                .exchange("/recipes/999", HttpMethod.DELETE, null, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<Recipe> getResponse = restTemplate
                .withBasicAuth("Peppa_Pig", "Peppa_pwd")
                .getForEntity("/recipes/999", Recipe.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldNotDeleteARecipeWithAnUnknownId() {
        ResponseEntity<Void> response = restTemplate
                .withBasicAuth("Peppa_Pig", "Peppa_pwd")
                .exchange("/recipes/9990", HttpMethod.DELETE, null, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<Recipe> getResponse = restTemplate
                .withBasicAuth("Peppa_Pig", "Peppa_pwd")
                .getForEntity("/recipes/9990", Recipe.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DirtiesContext
    void shouldUpdateARecipe() {
        Recipe newRecipe = new Recipe();
        newRecipe.setTitle("new title");
        newRecipe.setUrl(null);
        newRecipe.setDescription("new description");
        newRecipe.setSweet(false);
        newRecipe.setId(999L);

        ResponseEntity<String> response = restTemplate
                .withBasicAuth("Peppa_Pig", "Peppa_pwd")
                .exchange("/recipes/999", HttpMethod.PUT, new HttpEntity<>(newRecipe), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        compareRecipeObjectToResponse(response, newRecipe);

        response = restTemplate
                .withBasicAuth("Peppa_Pig", "Peppa_pwd")
                .getForEntity("/recipes/999", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        compareRecipeObjectToResponse(response, newRecipe);
    }

    @Test
    void shouldNotUpdateARecipeWithAnUnknownId() {
        Recipe newRecipe = new Recipe();
        newRecipe.setId(9990L);

        ResponseEntity<String> response = restTemplate
                .withBasicAuth("Peppa_Pig", "Peppa_pwd")
                .exchange("/recipes/9990", HttpMethod.PUT, new HttpEntity<>(newRecipe), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldNotUpdateARecipeOwnedByAnotherUser() {
        Recipe newRecipe = new Recipe();
        newRecipe.setId(9990L);

        ResponseEntity<String> response = restTemplate
                .withBasicAuth("Peppa_Pig", "Peppa_pwd")
                .exchange("/recipes/1002", HttpMethod.PUT, new HttpEntity<>(newRecipe), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private void compareRecipeObjectToResponse(ResponseEntity<String> response, Recipe recipe) {
        DocumentContext documentContext = JsonPath.parse(response.getBody());

        Number id = documentContext.read("$.id");
        if (recipe.getId() != null)
            assertThat(id.longValue()).isEqualTo(recipe.getId());
        else
            assertThat(id).isNotNull();

        String title = documentContext.read("$.title");
        assertThat(title).isEqualTo(recipe.getTitle());

        String description = documentContext.read("$.description");
        assertThat(description).isEqualTo(recipe.getDescription());

        String url = documentContext.read("$.url");
        assertThat(url).isEqualTo(recipe.getUrl());

        boolean isSweet = documentContext.read("$.isSweet");
        assertThat(isSweet).isEqualTo(recipe.isSweet());
    }

}
