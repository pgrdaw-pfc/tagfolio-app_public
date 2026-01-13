package com.pgrdaw.tagfolio.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * A DTO that holds a {@link Tag} and its associated usage counter.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Getter
@AllArgsConstructor
public class TagWithCounter {
    private Tag tag;
    private long counter;
}
