package ru.jamsys.core.flat.util;

import lombok.Getter;

import java.util.List;

//GPT generator
@Getter
public class Paginator<T> {


    private final List<T> items;           // Список элементов

    private final int itemsPerPage;        // Количество элементов на странице

    private final int totalPages;          // Общее количество страниц

    // Конструктор для инициализации пагинатора
    public Paginator(List<T> items, int itemsPerPage) {
        if (items == null || itemsPerPage <= 0) {
            throw new IllegalArgumentException("Список не может быть null и количество элементов на странице должно быть больше 0");
        }
        this.items = items;
        this.itemsPerPage = itemsPerPage;
        this.totalPages = (int) Math.ceil((double) items.size() / itemsPerPage); // Вычисляем количество страниц
    }

    // Метод для получения списка элементов на заданной странице
    public List<T> getPage(int pageNumber) {
        if (pageNumber < 1 || pageNumber > totalPages) {
            throw new IllegalArgumentException("Неверный номер страницы. Доступные страницы: 1 - " + totalPages);
        }
        // Вычисляем индекс первого элемента на текущей странице
        int startIndex = (pageNumber - 1) * itemsPerPage;
        // Вычисляем индекс последнего элемента на текущей странице
        int endIndex = Math.min(startIndex + itemsPerPage, items.size());

        return items.subList(startIndex, endIndex);
    }

    // Метод для проверки, является ли страница последней
    public boolean isLastPage(int pageNumber) {
        return pageNumber == totalPages;
    }

    // Метод для проверки, является ли страница первой
    public boolean isFirstPage(int pageNumber) {
        return pageNumber == 1;
    }

    public Integer getNextPage(int pageNumber) {
        if (isLastPage(pageNumber)) {
            return null;
        }
        return pageNumber + 1;
    }

    public Integer getPrevPage(int pageNumber) {
        if (isFirstPage(pageNumber)) {
            return null;
        }
        return pageNumber - 1;
    }

}
