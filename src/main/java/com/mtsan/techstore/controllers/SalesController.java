package com.mtsan.techstore.controllers;

import com.mtsan.techstore.Rank;
import com.mtsan.techstore.entities.Product;
import com.mtsan.techstore.entities.Sale;
import com.mtsan.techstore.entities.User;
import com.mtsan.techstore.exceptions.TechstoreDataException;
import com.mtsan.techstore.models.SaleModel;
import com.mtsan.techstore.repositories.ProductRepository;
import com.mtsan.techstore.repositories.SaleRepository;
import com.mtsan.techstore.repositories.UserRepository;
import com.mtsan.techstore.services.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.sql.Date;
import java.util.List;
import java.util.function.Predicate;

@RequestMapping("/sales")
@RestController
public class SalesController {

	private final SaleRepository saleRepository;

	private final UserRepository userRepository;

	private final ProductRepository productRepository;

	private final MailService mailService;

	@Autowired
	public SalesController(SaleRepository saleRepository, UserRepository userRepository, ProductRepository productRepository, MailService mailService) {
		this.saleRepository = saleRepository;
		this.userRepository = userRepository;
		this.productRepository = productRepository;
		this.mailService = mailService;
	}

	//fetching a list of all sales
	@RequestMapping(method = RequestMethod.GET)
	public ResponseEntity sales(@RequestParam(required = false) String start_date, @RequestParam(required = false) String end_date, @RequestParam(required = false) Long merchant_id, Authentication authentication) throws TechstoreDataException {
		if (saleRepository.count() > 0) {
			String rank = authentication.getAuthorities().toArray()[0].toString();
			if (rank.equals(Rank.Merchant.toString())) {
				//the authenticated user is Merchant, they can only see all their sales
				User merchant = userRepository.getUserByUsername(authentication.getName()).get(0);
				List<Sale> merchantSales = merchant.getSales();
				if(merchantSales.size() > 0) {
					return ResponseEntity.status(HttpStatus.OK).body(merchantSales);
				}
				throw new TechstoreDataException(HttpServletResponse.SC_NOT_FOUND, "You haven't sold anything yet");
			} else if (rank.equals(Rank.Administrator.toString())) {
				//the authenticated user is Administrator, they can analyze sales given dates and merchants
				if (merchant_id != null) {
					//we are going to return data for the merchant
					boolean isIdReal = userRepository.existsById(merchant_id);
					if (isIdReal) {
						User merchant = userRepository.findById(merchant_id).get();
						if (merchant.getRank() == Rank.Merchant) {
							if (start_date != null || end_date != null) {
								//the id we need is indeed for a merchant
								//and we also have a start and/or end date given
								Date startDate = null;
								Date endDate = null;
								try {
									if (start_date != null) {
										startDate = Date.valueOf(start_date);
									}
									if (end_date != null) {
										endDate = Date.valueOf(end_date);
									}
								}
								catch (IllegalArgumentException e) {
									throw new TechstoreDataException(HttpServletResponse.SC_BAD_REQUEST, "Bad Request");
								}

								List<Sale> merchantSales = merchant.getSales();
								final Date finalStartDate = startDate;
								final Date finalEndDate = endDate;

								merchantSales.removeIf(new Predicate<Sale>(){

									@Override
									public boolean test(Sale sale) {
										if(finalStartDate != null && finalEndDate != null) {
											return sale.getDateSold().compareTo(finalStartDate) < 0 || sale.getDateSold().compareTo(finalEndDate) > 0;
										} else if(finalStartDate != null) {
											return sale.getDateSold().compareTo(finalStartDate) < 0;
										} else if(finalEndDate != null) {
											return sale.getDateSold().compareTo(finalEndDate) > 0;
										}
										return false;
									}
								});
								if(merchantSales.size() > 0) {
									return ResponseEntity.status(HttpStatus.OK).body(merchantSales);
								}
								throw new TechstoreDataException(HttpServletResponse.SC_NOT_FOUND, "No sales found for the given time period and merchant");
							}

							List<Sale> listOfMerchantSales = merchant.getSales();
							if(listOfMerchantSales.size() > 0) {
								return ResponseEntity.status(HttpStatus.OK).body(merchant.getSales());
							}
							throw new TechstoreDataException(HttpServletResponse.SC_NOT_FOUND, "No sales found for the given merchant");
						} else {
							throw new TechstoreDataException(HttpServletResponse.SC_NOT_FOUND, "Invalid merchant ID");
						}
					} else {
						throw new TechstoreDataException(HttpServletResponse.SC_NOT_FOUND, "Merchant not found");
					}
				}
				if (start_date != null || end_date != null) {
					Date startDate = null;
					Date endDate = null;
					try {
						if (start_date != null) {
							startDate = Date.valueOf(start_date);
						}
						if (end_date != null) {
							endDate = Date.valueOf(end_date);
						}
					}
					catch (IllegalArgumentException e) {
						throw new TechstoreDataException(HttpServletResponse.SC_BAD_REQUEST, "Bad Request");
					}

					List<Sale> sales = null;

					if(startDate != null && endDate != null) {
						sales = saleRepository.getSalesByTimeRange(startDate, endDate);
					} else if(startDate != null) {
						sales = saleRepository.getSalesAfterDate(startDate);
					}
					else if(endDate != null) {
						sales = saleRepository.getSalesBeforeDate(endDate);
					}

					if (sales != null && sales.size() > 0) {
						return ResponseEntity.status(HttpStatus.OK).body(sales);
					} else {
						throw new TechstoreDataException(HttpServletResponse.SC_NOT_FOUND, "No sales found for the given time period");
					}
				}
				return ResponseEntity.status(HttpStatus.OK).body(saleRepository.findAll());
			} else {
				throw new TechstoreDataException(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
			}
		} else {
			throw new TechstoreDataException(HttpServletResponse.SC_NOT_FOUND, "No sales found");
		}
	}

	//selling a product
	@RequestMapping(consumes = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	public ResponseEntity sellProduct(@RequestBody SaleModel postedSale, Authentication authentication) throws TechstoreDataException {
		if (productRepository.count() > 0) {
			User merchant = userRepository.getUserByUsername(authentication.getName()).get(0);

			boolean isIdReal = productRepository.existsById(postedSale.getProductId());
			if (isIdReal) {
				Product product = productRepository.findById(postedSale.getProductId()).get();
				if(product.getQuantity() - postedSale.getQuantitySold() < 0 || product.getQuantity() - postedSale.getQuantitySold() > product.getQuantity()) {
					throw new TechstoreDataException(HttpServletResponse.SC_NOT_FOUND, "You can't sell more than " + product.getQuantity() + " of this product");
				}

				if(postedSale.getQuantitySold() == 0) {
					throw new TechstoreDataException(HttpServletResponse.SC_NOT_FOUND, "You need to sell more than 0 of this product");
				}

				product.setQuantity(product.getQuantity() - postedSale.getQuantitySold());
				if(product.getQuantity() <= product.getCriticalQuantity()) {
					List<User> admins = userRepository.getUsersByRank(Rank.Administrator);
					if(admins.size() > 0) {
						for(User admin : admins) {
							if(admin.getEmail() != null && admin.getEmail().length() > 0) {
								try {
									mailService.sendSimpleMessage(admin.getEmail(),
												"Depletion of " + product.getName(),
												"Dear " + admin.getDisplayName() + ",\n\n" +
													"You are receiving the following automatic email because the merchant \"" +
													merchant.getUsername() + "\" (\"" + merchant.getDisplayName() + "\", ID " + merchant.getId() + ") has just sold " +
													postedSale.getQuantitySold() + " of the product \"" + product.getName() + "\" (ID " + product.getId() + "). There are " + product.getQuantity() +
													" items left, whereas the critical quantity for the product is " + product.getCriticalQuantity() +
													".\nPlease restock ASAP.\n\nDo not reply to this email: it is sent automatically by the TechStore system.");
								}
								catch(Exception e) {
									System.out.println("Non-fatal error: the admin " + admin.getUsername() + " with email " + admin.getEmail() + " cannot receive the automated product depletion mail.");
								}
							}
						}
					}
				}

				Sale newSale = new Sale();
				newSale.setProduct(product);
				newSale.setQuantitySold(postedSale.getQuantitySold());
				newSale.setSellingMerchant(merchant);
				newSale.setPriceSold(product.getPricePerItem());
				newSale.setDateSold(new Date(System.currentTimeMillis()));

				Sale savedSale = saleRepository.save(newSale);
				productRepository.save(product);

				return ResponseEntity.status(HttpStatus.CREATED).body(savedSale);
			} else {
				throw new TechstoreDataException(HttpServletResponse.SC_NOT_FOUND, "Product not found");
			}
		} else {
			throw new TechstoreDataException(HttpServletResponse.SC_NOT_FOUND, "No products found");
		}
	}
}
