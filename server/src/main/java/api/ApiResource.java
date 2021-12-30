package api;

import business.ApplicationContext;
import business.category.Category;
import business.category.CategoryDao;
import business.book.Book;
import business.book.BookDao;
import business.customer.CustomerDao;
import business.order.*;
import com.mysql.cj.x.protobuf.MysqlxCrud;
import org.glassfish.jersey.jaxb.internal.XmlJaxbElementProvider;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.List;

@ApplicationPath("/")
@Path("/")
public class ApiResource {

    private final BookDao bookDao = ApplicationContext.INSTANCE.getBookDao();
    private final CategoryDao categoryDao = ApplicationContext.INSTANCE.getCategoryDao();
    private final CustomerDao customerDao = ApplicationContext.INSTANCE.getCustomerDao();
    private  final OrderDao orderDao = ApplicationContext.INSTANCE.getOrderDao();
    private final LineItemDao lineItemDao = ApplicationContext.INSTANCE.getLineItemDao();
//    private final OrderService orderService = ApplicationContext.INSTANCE.getOrderService();
    private final OrderService orderService = ApplicationContext.INSTANCE.getOrderService();
    @GET
    @Path("categories")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Category> categories(@Context HttpServletRequest httpRequest) {
        try {
            return categoryDao.findAll();
        } catch (Exception e) {
            throw new ApiException("categories lookup failed", e);
        }
    }

    @GET
    @Path("categories/{category-id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Category categoryById(@PathParam("category-id") long categoryId,
                                 @Context HttpServletRequest httpRequest) {
        try {
            Category result = categoryDao.findByCategoryId(categoryId);
            if (result == null) {
                throw new ApiException(String.format("No such category id: %d", categoryId));
            }
            return result;
        } catch (Exception e) {
            throw new ApiException(String.format("Category lookup by category-id %d failed", categoryId), e);
        }
    }

    @GET
    @Path("books/{book-id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Book bookById(@PathParam("book-id") long bookId,
                         @Context HttpServletRequest httpRequest) {
        try {
            Book result = bookDao.findByBookId(bookId);
            if (result == null) {
                throw new ApiException(String.format("No such book id: %d", bookId));
            }
            return result;
        } catch (Exception e) {
            throw new ApiException(String.format("Book lookup by book-id %d failed", bookId), e);
        }
    }



    @GET
    @Path("categories/{category-id}/books")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Book> booksByCategoryId(@PathParam("category-id") long categoryId,
                                        @Context HttpServletRequest httpRequest) {
        try {
            Category category = categoryDao.findByCategoryId(categoryId);
            if (category == null) {
                throw new ApiException(String.format("No such category id: %d", categoryId));
            }
            return bookDao.findByCategoryId(category.getCategoryId());
        } catch (Exception e) {
            throw new ApiException(String.format("Books lookup by category-id %d failed", categoryId), e);
        }
    }

    @GET
    @Path("categories/{category-id}/suggested-books")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Book> suggestedBooks(@PathParam("category-id") long categoryId,
                                     @QueryParam("limit") @DefaultValue("3") int limit,
                                     @Context HttpServletRequest request) {
        try {
            return bookDao.findRandomByCategoryId(categoryId, limit);
        } catch (Exception e) {
            throw new ApiException("products lookup via categoryID failed", e);
        }
    }

    @GET
    @Path("categories/name/{category-name}/suggested-books")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Book> suggestedBooksByName(@PathParam("category-name") String categoryName,
                                     @QueryParam("limit") @DefaultValue("3") int limit,
                                     @Context HttpServletRequest request) {
        try {
            Category result = categoryDao.findByName(categoryName);
            return bookDao.findRandomByCategoryName(result, limit);
        } catch (Exception e) {
            throw new ApiException("products lookup via categoryName failed", e);
        }
    }

    @GET
    @Path("categories/name/{category-name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Category categoryByName(@PathParam("category-name") String categoryName,
                                 @Context HttpServletRequest httpRequest) {
        try {
            Category result = categoryDao.findByName(categoryName);
            if (result == null) {
                throw new ApiException(String.format("No such category id: %s", categoryName));
            }
            return result;
        } catch (Exception e) {
            throw new ApiException(String.format("Category lookup by category-id %a failed", categoryName), e);
        }
    }
    @GET
    @Path("categories/name/{category-name}/books")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Book> booksByCategoryName(@PathParam("category-name") String categoryName,
                                        @Context HttpServletRequest httpRequest) {
        try {
            Category category = categoryDao.findByName(categoryName);
            if (category == null) {
                throw new ApiException(String.format("No such category id: %s", categoryName));
            }
            return bookDao.findByCategoryId(category.getCategoryId());
        } catch (Exception e) {
            throw new ApiException(String.format("Books lookup by category-id %s failed", categoryName), e);
        }
    }

    @POST
    @Path("orders")
    @Consumes(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public OrderDetails placeOrder(OrderForm orderForm) {

        try {
            long orderId = orderService.placeOrder(orderForm.getCustomerForm(), orderForm.getCart());
            if(orderId > 0){
                return orderService.getOrderDetails(orderId);
            }
            else {
                throw new ApiException.InvalidParameter("Unknown Error Occurred.");
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("order placement failed", e);
        }
    }
}
