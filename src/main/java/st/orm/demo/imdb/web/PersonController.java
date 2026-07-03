package st.orm.demo.imdb.web;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import st.orm.demo.imdb.service.PersonDetail;
import st.orm.demo.imdb.service.PersonService;

@Controller
public class PersonController {

    private final PersonService personService;

    public PersonController(PersonService personService) {
        this.personService = personService;
    }

    @GetMapping("/person/{personId}")
    public String personDetail(@PathVariable String personId, Model model) {
        PersonDetail detail = personService.findPersonDetail(personId);
        if (detail == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown person: " + personId);
        }
        model.addAttribute("person", detail.person());
        model.addAttribute("filmography", detail.filmography());
        model.addAttribute("statistics", detail.statistics());
        return "person";
    }
}
