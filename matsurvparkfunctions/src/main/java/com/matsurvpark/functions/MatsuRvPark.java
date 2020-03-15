package com.matsurvpark.functions;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXB;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import com.microsoft.azure.functions.annotation.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.matsurvpark.functions.reservation.Reservation;
import com.microsoft.azure.functions.*;
import com.microsoft.graph.authentication.IAuthenticationProvider;
import com.microsoft.graph.models.extensions.FieldValueSet;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.models.extensions.ListItem;
import com.microsoft.graph.requests.extensions.GraphServiceClient;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Blueprint for adding a reservations request
 *  to Matsu RV Park's sharepoint site list.
 * 
 * @version 1.0
 * @author jski-root
 * @since Feburary 2020
 */
public class MatsuRvPark {

	@FunctionName("make-reservation")
	public HttpResponseMessage run(@HttpTrigger(name = "req", methods = {
			HttpMethod.POST }, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
			final ExecutionContext context) {

		if (request.getBody() != null) {
			// process only if there is a request payload
			String body = request.getBody().get();
			
			//validate incoming request
			final List<SAXParseException> exceptions = new LinkedList<SAXParseException>();
			SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

			Schema schema;
			try {
				schema = factory.newSchema(new URL(Constants.schema));
				Validator validator = schema.newValidator();

				validator.setErrorHandler(new ErrorHandler() {

					@Override
					public void warning(SAXParseException exception) throws SAXException {
						exceptions.add(exception);
					}

					@Override
					public void error(SAXParseException exception) throws SAXException {
						exceptions.add(exception);
					}

					@Override
					public void fatalError(SAXParseException exception) throws SAXException {
						exceptions.add(exception);
					}
				});

				validator.validate(new StreamSource(new StringReader(body)));

			} catch (SAXException | IOException e1) {
				e1.printStackTrace();
				return request.createResponseBuilder(HttpStatus.SERVICE_UNAVAILABLE)
						 .body("could not process request, please call to make reservation.").build();
			}
			
			if(exceptions.size() > 0) {
				//invalid submission of data
				String errors = "";
				for(int i = 0; i < exceptions.size() && i < 20; i++) {
					errors += exceptions.get(i).getMessage() + "\n";
				}
				
				return request.createResponseBuilder(HttpStatus.NOT_ACCEPTABLE)
						 .body(errors).build();
			}
			
			try {
				// Convert incoming xml request to Bean/Class
				Reservation _reservation = JAXB.unmarshal(new StringReader(body), Reservation.class);
				
				// Get an access token for the client
				Auth authenticator = new Auth();
				IAuthenticationProvider authenticationProvider = authenticator.getAuthenticationProvider();

				// Create a new Graph client using the default client config and our auth
				// provider
				IGraphServiceClient graphClient = GraphServiceClient.builder()
						.authenticationProvider(authenticationProvider).buildClient();

				// Create a item on the reservation's sharepoint site.
				ListItem _li = new ListItem();
				FieldValueSet _f = new FieldValueSet();

				// convert Reservation Bean to JSON
				Gson gson = new GsonBuilder().setDateFormat("yyyyMMdd HH:mm:ss").create();
				JsonObject _r = new JsonParser().parse(gson.toJson(_reservation)).getAsJsonObject();
				UUID _uuid = UUID.randomUUID();
				Unique _uid = new Unique(_uuid.toString());
				JsonObject _u = new JsonParser().parse(gson.toJson(_uid)).getAsJsonObject();

				_f.additionalDataManager().put("Title", _u.get("uuid"));

				if (_r.get("customerName") != null && !"".equalsIgnoreCase(_reservation.getCustomerName()))
					_f.additionalDataManager().put("customer_name", _r.get("customerName"));

				if (_r.get("address") != null && !"".equalsIgnoreCase(_reservation.getAddress()))
					_f.additionalDataManager().put("address", _r.get("address"));

				if (_r.get("address2") != null && !"".equalsIgnoreCase(_reservation.getAddress2()))
					_f.additionalDataManager().put("address2", _r.get("address2"));

				if (_r.get("city") != null && !"".equalsIgnoreCase(_reservation.getCity()))
					_f.additionalDataManager().put("city", _r.get("city"));

				if (_r.get("state") != null && !"".equalsIgnoreCase(_reservation.getState()))
					_f.additionalDataManager().put("state", _r.get("state"));

				if (_r.get("zip") != null && !"".equalsIgnoreCase(_reservation.getZip()))
					_f.additionalDataManager().put("zip", _r.get("zip"));

				if (_r.get("rvModel") != null && !"".equalsIgnoreCase(_reservation.getRvModel()))
					_f.additionalDataManager().put("rv_model", _r.get("rvModel"));

				_f.additionalDataManager().put("number_of_persons", _r.get("numberOfPersons"));
				_f.additionalDataManager().put("creation_time", _r.get("creationTime"));

				if (_r.get("arrivalDate") != null && !"".equalsIgnoreCase(_reservation.getArrivalDate()))
					_f.additionalDataManager().put("arrival_date", _r.get("arrivalDate"));

				if (_r.get("departureDate") != null && !"".equalsIgnoreCase(_reservation.getDepartureDate()))
					_f.additionalDataManager().put("departure_date", _r.get("departureDate"));

				if (_r.get("contactEmail") != null && !"".equalsIgnoreCase(_reservation.getContactEmail()))
					_f.additionalDataManager().put("contact_email", _r.get("contactEmail"));

				if (_r.get("contactPhone") != null && !"".equalsIgnoreCase(_reservation.getContactPhone()))
					_f.additionalDataManager().put("contact_phone", _r.get("contactPhone"));


				if (_r.get("reservationType") != null && !"".equalsIgnoreCase(_reservation.getReservationType()))
					_f.additionalDataManager().put("reservation_type", _r.get("reservationType"));

				if (_r.get("additionalInformation") != null
						&& !"".equalsIgnoreCase(_reservation.getAdditionalInformation()))
					_f.additionalDataManager().put("additional_information", _r.get("additionalInformation"));

				_li.fields = _f;

				// submit item to list
				ListItem _new = graphClient.sites("root").lists("reservations").items().buildRequest().post(_li);

				// create response to client
				if (_new == null) {
					return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("could not add item to list")
							.build();
				} else {
					return request.createResponseBuilder(HttpStatus.OK).body(_uid.uuid).build();
				}
			} catch (FactoryConfigurationError | Exception e) {
				// error in converting request to Reservation bean
				e.printStackTrace();
				return request.createResponseBuilder(HttpStatus.NOT_ACCEPTABLE)
						.body(body + ":" + e.getLocalizedMessage()).build();
			}
		} else {
			// no data submitted, return error to client.
			return request.createResponseBuilder(HttpStatus.PRECONDITION_FAILED).body("no request sent").build();
		}
	}

	private class Unique {
		public String uuid;

		public Unique(String _uuid) {
			this.uuid = _uuid;
		}
	}
}