package springapp.recipes.service;

import org.springframework.stereotype.Service;
import springapp.recipes.model.Label;
import springapp.recipes.repository.LabelRepository;

import java.util.HashSet;
import java.util.Set;

@Service
public class LabelService {

    private final LabelRepository labelRepository;

    public LabelService(LabelRepository labelRepository) {
        this.labelRepository = labelRepository;
    }

    public Set<Label> createIfNotExist(Set<Label> labels) {

        Set<Label> existingLabels = new HashSet<>();

        for (Label label : labels) {
            if (label.getId() == null || !labelRepository.existsById(label.getId())) {
                label.setId(null);
                label = labelRepository.save(label);
            }
            existingLabels.add(label);
        }
        return existingLabels;
    }
}
