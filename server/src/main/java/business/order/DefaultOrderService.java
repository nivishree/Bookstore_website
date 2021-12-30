package business.order;

import api.ApiException;
import business.BookstoreDbException;
import business.JdbcUtils;
import business.book.Book;
import business.book.BookDao;
import business.cart.ShoppingCart;
import business.cart.ShoppingCartItem;
import business.customer.Customer;
import business.customer.CustomerDao;
import business.customer.CustomerForm;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.stream.Collectors;


public class DefaultOrderService implements OrderService {

	private BookDao bookDao;
	private CustomerDao customerDao;
	private OrderDao orderDao;
	private LineItemDao lineItemDao;


	public void setBookDao(BookDao bookDao) {
		this.bookDao = bookDao;
	}

	public void setCustomerDao(CustomerDao customerDao) {
		this.customerDao = customerDao;
	}

	public void setOrderDao(OrderDao orderDao) {
		this.orderDao = orderDao;
	}

	public  void setLineItemDao(LineItemDao lineItemDao){ this.lineItemDao = lineItemDao;}

	@Override
	public OrderDetails getOrderDetails(long orderId) {
		Order order = orderDao.findByOrderId(orderId);
		Customer customer = customerDao.findByCustomerId(order.getCustomerId());
		List<LineItem> lineItems = lineItemDao.findByOrderId(orderId);
		List<Book> books = lineItems
				.stream()
				.map(lineItem -> bookDao.findByBookId(lineItem.getBookId()))
				.collect(Collectors.toList());
		return new OrderDetails(order, customer, lineItems, books);
	}
	private Date getDate(String monthString, String yearString) {
		System.out.println("@@@Date month"+ monthString);
		System.out.println("@@@Date year"+ yearString);
		var month = Integer.parseInt(monthString);
		var year = Integer.parseInt(yearString);
		Date date = new GregorianCalendar(year, month - 1, 1).getTime();
		return date;
	}

	@Override
    public long placeOrder(CustomerForm customerForm, ShoppingCart cart) {

		validateCustomer(customerForm);
		validateCart(cart);

		try (Connection connection = JdbcUtils.getConnection()) {
			Date date = getDate(
					customerForm.getCcExpiryMonth(),
					customerForm.getCcExpiryYear());
			return performPlaceOrderTransaction(
					customerForm.getName(),
					customerForm.getAddress(),
					customerForm.getPhone(),
					customerForm.getEmail(),
					customerForm.getCcNumber(),
					date, cart, connection);
		} catch (SQLException e) {
			throw new BookstoreDbException("Error during close connection for customer order", e);
		}

	}

	private long performPlaceOrderTransaction(
			String name, String address, String phone,
			String email, String ccNumber, Date date,
			ShoppingCart cart, Connection connection) {
		try {
			connection.setAutoCommit(false);
			long customerId = customerDao.create(  // face issue now
					connection, name, address, phone, email,
					ccNumber, date);
			long customerOrderId = orderDao.create(
					connection,
					cart.getComputedSubtotal() + cart.getSurcharge(),
					generateConfirmationNumber(), customerId);
			for (ShoppingCartItem item : cart.getItems()) {
				lineItemDao.create(connection, customerOrderId,
						item.getBookId(), item.getQuantity());
			}
			connection.commit();
			return customerOrderId;
		} catch (Exception e) {
			try {
				connection.rollback();
			} catch (SQLException e1) {
				throw new BookstoreDbException("Failed to roll back transaction", e1);
			}
			return 0;
		}
	}

	private int generateConfirmationNumber() {
		int randomNumber = (int)Math.floor(Math.random() * 100);
		return randomNumber;
	}



	private void validateCustomer(CustomerForm customerForm) {

    	String name = customerForm.getName();

		if (!isNameValid(customerForm.getName())) {
			throw new ApiException.InvalidParameter("Invalid name field");
		}
		if (!isAddressValid(customerForm.getAddress())) {
			throw new ApiException.InvalidParameter("Invalid Address field");
		}
		if (!isPhoneValid(customerForm.getPhone())) {
			throw new ApiException.InvalidParameter("Invalid phone field");
		}
		if (!isEmailValid(customerForm.getEmail())) {
			throw new ApiException.InvalidParameter("Invalid email field");
		}
		if (!isCardValid(customerForm.getCcNumber())) {
			throw new ApiException.InvalidParameter("Invalid Card field");
		}
		if (!expiryDateIsInvalid(customerForm.getCcExpiryMonth(), customerForm.getCcExpiryYear())) {
			throw new ApiException.InvalidParameter("Invalid Expiry Date field");
		}



	}

	private boolean isPhoneValid(String phone) {
		if (phone == null) return false;
		if(phone.equals("")) return false;
		phone = phone.replaceAll(" ", "");
		phone = phone.replaceAll("-", "");
		phone = phone.replaceAll("\\(", "");
		phone = phone.replaceAll("\\)", "");
		if(!phone.matches("[0-9]+")) return false;
		if(phone.length() != 10) return false;
		return true;
	}

	private boolean isNameValid(String name) {
		if (name == null) return false;
		if(name.equals("")) return false;
		if(name.length() < 4) return  false;
		if(name.length() > 45) return  false;
		return true;
	}
	private boolean isAddressValid(String name) {
		if (name == null) return false;
		if(name.equals("")) return false;
		if(name.length() < 4) return  false;
		if(name.length() > 45) return  false;
		return true;
	}
	private boolean isEmailValid(String email) {
		if (email == null) return false;
		if(email.equals("")) return false;
		if(email.length() < 4) return  false;
		if(email.length() > 45) return  false;
		if(!email.contains("@")) return false;
		if(email.contains(" ")) return false;
		if(email.endsWith(".")) return false;
		return true;
	}
	private boolean isCardValid(String card) {
		if (card == null) return false;
		if(card.equals("")) return false;
		card = card.replaceAll(" ", "");
		card = card.replaceAll("-", "");
		if(!card.matches("[0-9]+")) return false;
		if(card.length() > 16) return false;
		if(card.length() < 14) return false;
		return true;
	}

	private boolean expiryDateIsInvalid(String ccExpiryMonth, String ccExpiryYear) {
		YearMonth y = YearMonth.now();
		YearMonth yearMonth1
				= YearMonth.of(Integer.parseInt(ccExpiryYear), Integer.parseInt(ccExpiryMonth));

		if (!(yearMonth1
				.isAfter(y))) {
			if (yearMonth1
					.equals(y)) {
				return true;

			} else {
				return false;
			}
		}
		return true;
	}

	private void validateCart(ShoppingCart cart) {


		if (cart.getItems().size() <= 0) {
			throw new ApiException.InvalidParameter("Cart is empty.");
		}

		cart.getItems().forEach(item -> {
			if (item.getQuantity() < 0 || item.getQuantity() > 99) {
				throw new ApiException.InvalidParameter("Invalid quantity");
			}
		});
		cart.getItems().forEach(book -> {
			if (book.getBookForm().getPrice() != bookDao.findByBookId(book.getBookId()).getPrice()) {
				throw new ApiException.InvalidParameter("Invalid price of the book");
			}

		});
		cart.getItems().forEach(book -> {
			if (book.getBookForm().getCategoryId() != bookDao.findByBookId(book.getBookId()).getCategoryId()) {
				throw new ApiException.InvalidParameter("Invalid category of the book");
			}
		});
	}
}
