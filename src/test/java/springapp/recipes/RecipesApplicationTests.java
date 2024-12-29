package springapp.recipes;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RecipesApplicationTests {

    @Autowired
    TestRestTemplate restTemplate;

    @Test void shouldReturnRecipesList() {
        ResponseEntity<String> response = restTemplate.getForEntity("/recipes", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        DocumentContext documentContext = JsonPath.parse(response.getBody());
        System.out.println(documentContext);
        int recipeCount = documentContext.read("$.length()");
        assertThat(recipeCount).isEqualTo(3);

        JSONArray ids = documentContext.read("$..id");
        assertThat(ids).containsExactlyInAnyOrder(999, 1000, 1001);

        JSONArray titles = documentContext.read("$..title");
        assertThat(titles).containsExactlyInAnyOrder("Makownik", "Paszteciki ze szpinakiem", "Lukier cytrynowy");

        JSONArray urls = documentContext.read("$..url");
        assertThat(urls).containsExactlyInAnyOrder("https://kuchnia-domowa-ani.blogspot.com/2018/12/paszteciki-ze-szpinakiem-i-serem.html", "https://ilovebake.pl/przepis/makowiec-na-kruchym-spodzie", null);

        JSONArray areSweet = documentContext.read("$..isSweet");
        assertThat(areSweet).containsExactlyInAnyOrder(true, true, false);

        // TODO: how to test description?
    }

    @Test
    void shouldReturnAPageOfRecipes() {
        ResponseEntity<String> response = restTemplate
                .getForEntity("/recipes?page=0&size=1", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        DocumentContext documentContext = JsonPath.parse(response.getBody());
        JSONArray page = documentContext.read("$[*]");
        assertThat(page.size()).isEqualTo(1);
    }

    @Test
    void shouldReturnASortedPageOfRecipes() {
        ResponseEntity<String> response = restTemplate
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
        ResponseEntity<String> response = restTemplate
                .getForEntity("/recipes", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        DocumentContext documentContext = JsonPath.parse(response.getBody());
        JSONArray page = documentContext.read("$[*]");
        assertThat(page.size()).isEqualTo(3);

        JSONArray titles = documentContext.read("$..title");
        assertThat(titles).containsExactly("Lukier cytrynowy", "Makownik", "Paszteciki ze szpinakiem");
    }

    @Test
    void shouldReturnARecipeWhenDataIsSaved() {
        ResponseEntity<String> response = restTemplate
                .getForEntity("/recipe/999", String.class);
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
        ResponseEntity<String> response = restTemplate
                .getForEntity("/recipe/5000", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DirtiesContext
    void shouldCreateANewCashCard() {

        Recipe newRecipe = new Recipe();
        newRecipe.setTitle("title");
        newRecipe.setUrl("url");
        newRecipe.setDescription("description");
        newRecipe.setSweet(false);

        ResponseEntity<Void> createResponse = restTemplate
                .postForEntity("/recipe", newRecipe, Void.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        URI locationOfNewRecipe = createResponse.getHeaders().getLocation();
        ResponseEntity<String> getResponse = restTemplate
                .getForEntity(locationOfNewRecipe, String.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        DocumentContext documentContext = JsonPath.parse(getResponse.getBody());
        Number id = documentContext.read("$.id");
        String title = documentContext.read("$.title");
        String url = documentContext.read("$.url");
        String description = documentContext.read("$.description");
        boolean isSweet = documentContext.read("$.isSweet");

        assertThat(id).isNotNull();
        assertThat(title).isEqualTo("title");
        assertThat(url).isEqualTo("url");
        assertThat(description).isEqualTo("description");
        assertThat(isSweet).isFalse();
    }
}
