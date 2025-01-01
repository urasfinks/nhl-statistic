package ru.jamsys.core.flat.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.extension.builder.ArrayListBuilder;

import static org.junit.jupiter.api.Assertions.assertNull;

class PaginatorTest {

    @Test
    void test() {
        Paginator<String> paginator = new Paginator<>(new ArrayListBuilder<String>()
                .append("One")
                .append("two")
                .append("three"),
                2
        );
        Assertions.assertEquals(2, paginator.getTotalPages());
        Assertions.assertEquals("[One, two]", paginator.getPage(1).toString());
        Assertions.assertEquals("[three]", paginator.getPage(2).toString());
        Assertions.assertTrue(paginator.isFirstPage(1));
        Assertions.assertFalse(paginator.isFirstPage(2));
        Assertions.assertTrue(paginator.isLastPage(2));
        Assertions.assertFalse(paginator.isLastPage(1));

        assertNull(paginator.getPrevPage(1));
        Assertions.assertEquals(1, paginator.getPrevPage(2));
        assertNull(paginator.getNextPage(2));
        Assertions.assertEquals(2, paginator.getNextPage(1));
    }

}