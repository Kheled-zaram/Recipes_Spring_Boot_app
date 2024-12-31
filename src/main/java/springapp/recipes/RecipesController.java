package springapp.recipes;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.security.Principal;
import java.util.Date;

@RestController
@RequestMapping("/recipes")
public class RecipesController {

    private final RecipeRepository recipeRepository;

    public RecipesController(RecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
    }

    @GetMapping
    public ResponseEntity<Iterable<Recipe>> findAllRecipes(Pageable pageable, Principal principal) {
        Page<Recipe> page = recipeRepository.findByOwner(
                principal.getName(),
                PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        pageable.getSortOr(Sort.by(Sort.Direction.ASC, "lastUpdate"))
                ));
        return ResponseEntity.ok(page.getContent());
    }

    @PostMapping
    public ResponseEntity<Void> saveRecipe(@RequestBody Recipe recipe, UriComponentsBuilder ucb, Principal principal) {
        recipe.setOwner(principal.getName());
        recipe.setLastUpdate(new Date());

        Recipe savedRecipe = recipeRepository.save(recipe);
        URI locationOfNewCashCard = ucb
                .path("recipes/{id}")
                .buildAndExpand(savedRecipe.getId())
                .toUri();
        return ResponseEntity.created(locationOfNewCashCard).build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Recipe> findRecipe(@PathVariable Long id, Principal principal) {
        Recipe recipe = recipeRepository.findByIdAndOwner(id, principal.getName());

        if (recipe != null)
            return ResponseEntity.ok(recipe);

        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecipe(@PathVariable Long id, Principal principal) {

        if (!recipeRepository.existsByIdAndOwner(id, principal.getName()))
            return ResponseEntity.notFound().build();

        recipeRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Recipe> updateRecipe(@PathVariable Long id, @RequestBody Recipe recipe, Principal principal) {
        recipe.setOwner(principal.getName());
        recipe.setLastUpdate(new Date());

        if (!recipeRepository.existsByIdAndOwner(id, principal.getName()))
            return ResponseEntity.notFound().build();

        recipeRepository.save(recipe);
        return ResponseEntity.ok(recipe);
    }

}

