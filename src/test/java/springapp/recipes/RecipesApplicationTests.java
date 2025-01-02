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
import springapp.recipes.model.Category;
import springapp.recipes.model.Label;
import springapp.recipes.model.Recipe;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

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

        JSONArray ids = documentContext.read("$.*.id");
        assertThat(ids).containsExactlyInAnyOrder(999, 1000, 1001);

        JSONArray titles = documentContext.read("$..title");
        assertThat(titles).containsExactlyInAnyOrder("Makownik", "Paszteciki ze szpinakiem", "Lukier cytrynowy");

        JSONArray urls = documentContext.read("$..url");
        assertThat(urls)
                .containsExactlyInAnyOrder("https://kuchnia-domowa-ani.blogspot.com/2018/12/paszteciki-ze-szpinakiem-i-serem.html",
                        "https://ilovebake.pl/przepis/makowiec-na-kruchym-spodzie", null);

        JSONArray areSweet = documentContext.read("$..isSweet");
        assertThat(areSweet).containsExactlyInAnyOrder(true, true, false);

        JSONArray category_ids = documentContext.read("$.*.category.id");
        assertThat(category_ids).containsExactlyInAnyOrder(11, 12, 13);
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

        Recipe expected = getSavedRecipe();

        compareRecipeObjectToResponse(response, expected);
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
        Recipe newRecipe = getSavedRecipe();

        newRecipe.setTitle("title");
        newRecipe.setUrl("url");
        newRecipe.setDescription("description");
        newRecipe.setSweet(false);
        newRecipe.setId(null);

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

        compareLabelsWithResponse(locationOfNewRecipe.getPath(), getSavedLabels());
    }

    @Test
    @DirtiesContext
    void shouldCreateANewRecipeAndANewCategoryAndANewLabel() {
        Recipe newRecipe = getSavedRecipe();

        newRecipe.setTitle("title");
        newRecipe.setUrl("url");
        newRecipe.setDescription("description");
        newRecipe.setSweet(false);
        newRecipe.setId(null);

        Category newCategory = new Category();
        newCategory.setName("category_name");
        newRecipe.setCategory(newCategory);

        Label newLabel = new Label();
        newLabel.setName("New label");

        Set<Label> labels = new HashSet<>(getSavedLabels());
        labels.add(newLabel);
        newRecipe.setLabels(labels);

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

        compareLabelsWithResponse(locationOfNewRecipe.getPath(), labels);
    }

    @Test
    @DirtiesContext
    void shouldCreateANewRecipeWithVeryLongDescription() throws IOException {
        Recipe newRecipe = getSavedRecipe();
        newRecipe.setId(null);

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
        Recipe newRecipe = getSavedRecipe();
        newRecipe.setTitle("new title");
        newRecipe.setUrl(null);
        newRecipe.setDescription("new description");
        newRecipe.setSweet(false);

        Category newCategory = new Category();
        newCategory.setId(13);
        newCategory.setName("Drożdżówki");
        newRecipe.setCategory(newCategory);

        Label label0 = new Label();
        label0.setName("New label");

        Label label1 = new Label();
        label1.setId(11);
        label1.setName("Mak");
        newRecipe.setLabels(Set.of(label0, label1));

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

        compareLabelsWithResponse("/recipes/999", Set.of(label0, label1));
    }

    @Test
    void shouldNotUpdateARecipeWithAnUnknownId() {
        Recipe newRecipe = getSavedRecipe();
        newRecipe.setId(9990L);

        ResponseEntity<String> response = restTemplate
                .withBasicAuth("Peppa_Pig", "Peppa_pwd")
                .exchange("/recipes/9990", HttpMethod.PUT, new HttpEntity<>(newRecipe), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldNotUpdateARecipeOwnedByAnotherUser() {
        Recipe newRecipe = getSavedRecipe();
        newRecipe.setId(9990L);

        ResponseEntity<String> response = restTemplate
                .withBasicAuth("Peppa_Pig", "Peppa_pwd")
                .exchange("/recipes/1002", HttpMethod.PUT, new HttpEntity<>(newRecipe), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldReturnLabelsOfARecipe() {
        compareLabelsWithResponse("/recipes/999", getSavedLabels());
    }

    @Test
    void shouldNotReturnLabelsOfARecipeOwnedByAnotherUser() {
        ResponseEntity<String> response = restTemplate
                .withBasicAuth("Mummy_Pig", "Mummy_pwd")
                .getForEntity("/recipes/999/labels", String.class);
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

        Number category_id = documentContext.read("$.category.id");
        if (recipe.getCategory().getId() != null)
            assertThat(category_id).isEqualTo(recipe.getCategory().getId());
        else
            assertThat(category_id).isNotNull();

        String category_name = documentContext.read("$.category.name");
        assertThat(category_name).isEqualTo(recipe.getCategory().getName());
    }

    private void compareLabelsWithResponse(String recipeLocation, Set<Label> labels) {
        ResponseEntity<String> response = restTemplate
                .withBasicAuth("Peppa_Pig", "Peppa_pwd")
                .getForEntity(recipeLocation + "/labels", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        DocumentContext documentContext = JsonPath.parse(response.getBody());
        JSONArray ids = documentContext.read("$.*.id");
        assertThat(ids).contains(labels.stream().map(Label::getId).filter(Objects::nonNull).toArray());
        assertThat(ids).doesNotContainNull();

        JSONArray names = documentContext.read("$.*.name");
        assertThat(names).containsExactlyInAnyOrder(labels.stream().map(Label::getName).toArray());
    }

    private Recipe getSavedRecipe() {
        Recipe expected = new Recipe();
        expected.setId(999L);
        expected.setTitle("Makownik");
        expected.setUrl("https://ilovebake.pl/przepis/makowiec-na-kruchym-spodzie");
        expected.setSweet(true);

        Category category = new Category();
        category.setId(11);
        category.setName("Ciasta");
        expected.setCategory(category);

        expected.setLabels(getSavedLabels());
        return expected;
    }

    private Set<Label> getSavedLabels() {
        Label label1 = new Label();
        label1.setId(11);
        label1.setName("Mak");

        Label label2 = new Label();
        label2.setId(12);
        label2.setName("Drożdże");

        Label label3 = new Label();
        label3.setId(13);
        label3.setName("Świąteczne wypieki");

        return Set.of(label1, label2, label3);
    }

}
