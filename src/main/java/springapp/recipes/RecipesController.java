package springapp.recipes;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Optional;

@RestController
public class RecipesController {

    private final RecipeRepository recipeRepository;

    public RecipesController(RecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
    }

    @GetMapping("/recipes")
    public ResponseEntity<Iterable<Recipe>> findAllRecipes(Pageable pageable) {
        Page<Recipe> page = recipeRepository.findAll(
                PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        pageable.getSortOr(Sort.by(Sort.Direction.ASC, "title"))
                ));
        return ResponseEntity.ok(page.getContent());
    }

    @PostMapping("/recipe")
    public ResponseEntity<Void> saveRecipe(@RequestBody Recipe recipe, UriComponentsBuilder ucb) {
        Recipe savedRecipe = recipeRepository.save(recipe);
        URI locationOfNewCashCard = ucb
                .path("recipe/{id}")
                .buildAndExpand(savedRecipe.getId())
                .toUri();
        return ResponseEntity.created(locationOfNewCashCard).build();
    }

    @GetMapping("/recipe/{id}")
    public ResponseEntity<Recipe> findRecipe(@PathVariable Long id) {
        Optional<Recipe> recipe = recipeRepository.findById(id);
//        return recipe.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
        if (recipe.isPresent()) {
            return ResponseEntity.ok(recipe.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }


}

