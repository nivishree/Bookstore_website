package business.book;

import business.category.Category;

import java.util.List;

public interface BookDao {

    public Book findByBookId(long bookId);

    public List<Book> findByCategoryId(long categoryId);

    public List<Book> findRandomByCategoryId(long categoryId, int limit);

    public List<Book> findRandomByCategoryName(Category categoryName, int limit);

}
