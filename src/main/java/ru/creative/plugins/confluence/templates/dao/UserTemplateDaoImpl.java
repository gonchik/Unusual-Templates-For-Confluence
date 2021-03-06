package ru.creative.plugins.confluence.templates.dao;


import com.atlassian.activeobjects.external.ActiveObjects;
import lombok.extern.slf4j.Slf4j;
import net.java.ao.DBParam;
import net.java.ao.Query;
import ru.creative.plugins.confluence.templates.dto.UserTemplateDto;
import ru.creative.plugins.confluence.templates.model.AbstractTemplate;
import ru.creative.plugins.confluence.templates.model.Tag;
import ru.creative.plugins.confluence.templates.model.UserTemplate;
import ru.creative.plugins.confluence.templates.util.StatusModel;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class UserTemplateDaoImpl implements UserTemplateDao {
    private final ActiveObjects entityManager;
    private final TagDao tagDao;
    private final AbstractTemplateDao templateDao;

    public UserTemplateDaoImpl(ActiveObjects entityManager, TagDao tagDao, AbstractTemplateDao templateDao) {
        this.entityManager = checkNotNull(entityManager);
        this.tagDao = checkNotNull(tagDao);
        this.templateDao = checkNotNull(templateDao);
    }


    @Override
    public UserTemplate getUserTemplate(Integer id) {
        return templateDao.getTemplate(id, UserTemplate.class);
    }

    @Override
    public List<UserTemplate> getUserCreatedTemplates(String creator) {
        final Query query = Query.select().where("CREATOR = ?", creator).order("ID DESC");
        return Arrays.asList(entityManager.find(UserTemplate.class, query));
    }

    @Override
    public UserTemplate addUserTemplate(final UserTemplateDto dto) {
        final UserTemplate template = entityManager.create(UserTemplate.class,
                new DBParam("NAME", dto.getName()),
                new DBParam("DESCR", dto.getDescription()),
                new DBParam("BODY", dto.getBody()),
                new DBParam("CREATOR", dto.getCreator()),
                new DBParam("STATUS", StatusModel.NEW.toString())
        );
        template.save();
        dto.getTags().forEach(
                tag -> tagDao.associateTagToTemplate(tagDao.getOrCreateTag(tag), template)
        );
        return getUserTemplate(template.getID());
    }

    @Override
    public UserTemplate updateUserTemplate(UserTemplateDto dto) {
        return entityManager.executeInTransaction( () -> {
            final UserTemplate template = getUserTemplate(dto.getId());
            template.setName(dto.getName());
            template.setBody(dto.getBody());
            template.setDescription(dto.getDescription());
            template.setCreator(dto.getCreator());
            //remove existing associations
            templateDao.smartTagAssociationsRemove(template, dto.getTags());
            template.save();
            return getUserTemplate(template.getID());
        });
    }

    @Override
    public void deleteTemplate(AbstractTemplate template) {
        templateDao.deleteTemplate(template);
    }

}
