package com.company.commitet_jm.view.project

import com.company.commitet_jm.entity.Project
import com.company.commitet_jm.view.main.MainView
import com.vaadin.flow.component.ClickEvent
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.H3
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.Route
import io.jmix.core.DataManager
import io.jmix.core.FetchPlan
import io.jmix.flowui.ViewNavigators
import io.jmix.flowui.kit.component.button.JmixButton
import io.jmix.flowui.model.CollectionContainer
import io.jmix.flowui.view.*
import org.springframework.beans.factory.annotation.Autowired

@Route(value = "projects-cards", layout = MainView::class)
@ViewController(id = "Project.cards")
@ViewDescriptor(path = "project-card-view.xml")
class ProjectCardView : StandardView() {

    @Autowired
    private lateinit var dataManager: DataManager

    @Autowired
    private lateinit var viewNavigators: ViewNavigators

    @ViewComponent
    private lateinit var projectsContainer: VerticalLayout

    @ViewComponent
    private lateinit var projectsDc: CollectionContainer<Project>

    @Subscribe
    fun onBeforeShow(event: BeforeShowEvent) {
        loadProjects()
        renderProjectCards()
    }

    @Subscribe("createButton")
    fun onCreateButtonClick(event: ClickEvent<JmixButton>) {
        viewNavigators.detailView(this, Project::class.java)
            .newEntity()
            .withBackwardNavigation(true)
            .navigate()
    }

    @Subscribe("refreshButton")
    fun onRefreshButtonClick(event: ClickEvent<JmixButton>) {
        loadProjects()
        renderProjectCards()
    }

    private fun loadProjects() {
        val projects = dataManager.load(Project::class.java)
            .all()
            .fetchPlan { fp ->
                fp.addFetchPlan(FetchPlan.BASE)
                fp.add("platform", FetchPlan.BASE)
            }
            .list()
        projectsDc.setItems(projects)
    }

    private fun renderProjectCards() {
        projectsContainer.removeAll()

        // Создаём контейнер для сетки карточек
        val gridLayout = Div().apply {
            style.set("display", "grid")
            style.set("grid-template-columns", "repeat(auto-fill, minmax(300px, 1fr))")
            style.set("gap", "1.5rem")
            style.set("padding", "1rem")
        }

        projectsDc.items.forEach { project ->
            gridLayout.add(createProjectCard(project))
        }

        projectsContainer.add(gridLayout)
    }

    private fun createProjectCard(project: Project): Div {
        return Div().apply {
            // Стили карточки
            style.set("border", "1px solid var(--lumo-contrast-20pct)")
            style.set("border-radius", "var(--lumo-border-radius-m)")
            style.set("padding", "1.5rem")
            style.set("background", "var(--lumo-base-color)")
            style.set("cursor", "pointer")
            style.set("transition", "box-shadow 0.2s, transform 0.2s")

            // Hover эффект
            element.addEventListener("mouseenter") {
                style.set("box-shadow", "var(--lumo-box-shadow-m)")
                style.set("transform", "translateY(-2px)")
            }
            element.addEventListener("mouseleave") {
                style.set("box-shadow", "none")
                style.set("transform", "translateY(0)")
            }

            // Контент карточки
            val nameLabel = H3(project.name ?: "Без названия").apply {
                style.set("margin", "0 0 1rem 0")
                style.set("color", "var(--lumo-primary-text-color)")
            }

            val platformText = if (project.platform != null) {
                "${project.platform?.name ?: ""} ${project.platform?.version ?: ""}".trim()
            } else {
                "Платформа не указана"
            }

            val platformBadge = Span(platformText).apply {
                element.themeList.add("badge")
                element.themeList.add("primary")
                style.set("margin-bottom", "0.5rem")
                style.set("display", "inline-block")
            }

            val urlLabel = Span(project.urlRepo ?: "URL не указан").apply {
                style.set("font-size", "var(--lumo-font-size-s)")
                style.set("color", "var(--lumo-secondary-text-color)")
                style.set("display", "block")
                style.set("margin-top", "0.5rem")
                style.set("word-break", "break-all")
            }

            // Клик по карточке - открыть детальную страницу проекта
            element.addEventListener("click") {
                viewNavigators.detailView(this@ProjectCardView, Project::class.java)
                    .withBackwardNavigation(true)
                    .editEntity(project)
                    .navigate()
            }

            add(nameLabel, platformBadge, urlLabel)
        }
    }
}
