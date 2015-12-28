"""
stops.py - Google App Engine application for stops data collection, views.
Copyright 2015 Michael Farrell <micolous+git@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
"""
import webapp2, jinja2, os, urllib, csv, uuid

from google.appengine.api import users
from google.appengine.ext import ndb

from webapp2_extras.appengine import sessions_memcache
from webapp2_extras import sessions

from models import Agency, Stop, Profile, StopReport, VEHICLE_TYPES


JINJA = jinja2.Environment(
	loader=jinja2.FileSystemLoader(os.path.join(os.path.dirname(__file__), 'templates')),
	extensions=['jinja2.ext.autoescape'],
	autoescape=True)

CONFIG = {}
CONFIG['webapp2_extras.sessions'] = dict(secret_key=os.environ['SECRET_KEY'])


def csrf_protect(handler):
	def inner(self, *args, **kwargs):
		token = self.request.params.get('csrf')
		if token and self.session.get('csrf') == token:
			self.session['csrf'] = uuid.uuid4().hex
			handler(self, *args, **kwargs)
		else:
			self.abort(400)
	return inner

class BaseHandler(webapp2.RequestHandler):
	def dispatch(self):
		# Get a session store for this request.
		self.session_store = sessions.get_store(request=self.request)

		try:
			# Dispatch the request.
			webapp2.RequestHandler.dispatch(self)
		finally:
			# Save all sessions.
			self.session_store.save_sessions(self.response)

	@webapp2.cached_property
	def session(self):
		# Returns a session using the default cookie key.
		return self.session_store.get_session(
			name='mc_session',
			factory=sessions_memcache.MemcacheSessionFactory)

	def render_template(self, template_name, context=None):
		user = users.get_current_user()
		if context is None:
			context = dict()

		if not self.session.get('csrf'):
			self.session['csrf'] = uuid.uuid4().hex

		# Standard context
		context = dict(context,
			user_name=user.nickname(),
			logout_url=users.create_logout_url(self.request.uri),
			csrf=self.session.get('csrf'),
		)

		template = JINJA.get_template(template_name)
		return template.render(context)

	def render_to_response(self, template_name, context=None):
		self.response.write(self.render_template(template_name, context))

	def send_404(self):
		self.render_to_response('404.html')
		self.abort(400)


class AgencyListView(BaseHandler):
	def get(self):
		profile = Profile.get_or_create()
		assert profile is not None
		
		agencies = Agency.query(Agency.enabled == True)
		if agencies.count(limit=1) < 1:
			agencies = None

		context = dict(
			object_list=agencies,
			report_count=StopReport.query(StopReport.user == profile.key).count(limit=100),
		)
		self.render_to_response('agency_list.html', context)


class AgencyDetailView(BaseHandler):
	def get(self, agency_id):
		try:
			agency = Agency.get_by_id(int(agency_id))
			if agency is None:
				return send_404(self)
		except:
			return send_404(self)

		agency_vehicle_types = list(agency.vehicle_types)
		agency_vehicle_types.sort()
		agency_vehicle_types = [(x, VEHICLE_TYPES[x]) for x in agency_vehicle_types]
		
		context = dict(
			agency_name=agency.name,
			vehicle_types=agency_vehicle_types,
		)
		self.render_to_response('agency_detail.html', context)

class AgencyReportView(BaseHandler):
	def get(self, agency_id, vehicle):
		try:
			agency = Agency.get_by_id(int(agency_id))
			if agency is None or vehicle not in VEHICLE_TYPES or vehicle not in agency.vehicle_types:
				return self.send_404()
		except:
			return self.send_404()

		# Check for known stops
		known_stops = list(Stop.query(Stop.stop_type == vehicle, Stop.agency == agency.key).order(Stop.name))

		context = dict(
			agency_name=agency.name,
			agency_id=agency.key.id(),
			vehicle=vehicle,
			known_stops=known_stops,
		)
		self.render_to_response('agency_report.html', context)

class SubmitReportView(BaseHandler):
	def _retry_form(self, agency, vehicle, card_id, known_stop, stop_name, comment, error_msg):
		known_stops = list(Stop.query(Stop.stop_type == vehicle, Stop.agency == agency.key).order(Stop.name))

		context = dict(
		    agency_name=agency.name,
			agency_id=agency.key.id(),
			vehicle=urllib.quote_plus(vehicle),
			card_id=urllib.quote_plus(str(card_id)),
			known_stop=known_stop, # select element
			stop_name=urllib.quote_plus(stop_name),
			comment=comment, # textarea
			error_msg=error_msg,
			known_stops=known_stops,
		)
		
		self.render_to_response('agency_report.html', context)

	@csrf_protect
	def post(self):
		profile = Profile.get_or_create()
		assert profile is not None

		agency_id = self.request.get('agency_id')
		vehicle = self.request.get('vehicle')
		card_id = self.request.get('card_id')
		known_stop = self.request.get('known_stop')
		stop_name = self.request.get('stop_name')
		comment = self.request.get('comment')

		# Start validation!
		# Check agency
		try:
			agency = Agency.get_by_id(int(agency_id))
			if agency is None or not agency.enabled:
				# Invalid agency
				return self.send_404()
		except:
			return self.send_404()

		# Check vehicle type
		if vehicle not in VEHICLE_TYPES or vehicle not in agency.vehicle_types:
			return self.send_404()

		# Check card id
		try:
			card_id = int(card_id)
		except:
			# invalid card id, not a number.
			return self._retry_form(agency, vehicle, card_id, known_stop, stop_name, comment, 'Card ID is not a number')

		# Check known_stop
		if known_stop != '':
			try:
				known_stop = Stop.get_by_id(int(known_stop))
				if known_stop.agency.id() != agency.key.id():
					# stop is not for agency
					known_stop = None
			except:
				
				known_stop = None
		
		if known_stop is None:
			# Invalid stop
			return self._retry_form(agency, vehicle, card_id, known_stop, stop_name, comment, 'Stop ID is not valid')

		if known_stop == '':
			known_stop = None

		# Check stop_name is present if known_stop is not
		if known_stop is None and stop_name == '':
			# Custom name not specified and no known stop selected.
			return self._retry_form(agency, vehicle, card_id, known_stop, stop_name, comment, 'No stop name was entered')

		# If the user is banned, then say we processed the report, but don't
		# actually store it anywhere.
		if not profile.banned:
			# Now get the extra metadata and make the report
			country = self.request.headers.get('x-appengine-country', 'XX')
			region = self.request.headers.get('x-appengine-region', '')
			city = self.request.headers.get('x-appengine-city', '')
			ip = self.request.remote_addr
		
			report = StopReport(
				agency=agency.key,
				stop=known_stop.key if known_stop else None,
				name=stop_name,
				stop_type=vehicle,
				card_id=str(card_id),
				comment=comment,
				gae_country=country,
				gae_region=region,
				gae_city=city,
				gae_ip=ip,
				user=profile.key
			)
			report.put()
		
		context = dict(
			agency_name=agency.name,
			agency_id=agency.key.id(),
		)
		self.render_to_response('report_sent.html', context)


class AgencyEditView(BaseHandler):
	def get(self, agency_id):
		user = users.get_current_user()
		if not user or not users.is_current_user_admin():
			return

		try:
			agency = Agency.get_by_id(int(agency_id))
			if agency is None:
				return self.send_404()
		except:
			return self.send_404()

		context = dict(
			agency_name=agency.name,
			agency=agency,
			vehicle_types=VEHICLE_TYPES.keys(),
		)
		
		self.render_to_response('agency_edit.html', context)

	@csrf_protect
	def post(self, agency_id):
		user = users.get_current_user()
		if not user or not users.is_current_user_admin():
			return

		try:
			agency = Agency.get_by_id(int(agency_id))
			if agency is None:
				return self.send_404()
		except:
			return self.send_404()

		vehicle_types = [x.strip().lower() for x in self.request.get('vehicle_types').split(',')]
		vehicle_types = [x for x in vehicle_types if x in VEHICLE_TYPES.keys()]
		agency.vehicle_types = vehicle_types
		agency.put()

		context = dict(
			agency_name=agency.name,
			agency=agency,
			vehicle_types=VEHICLE_TYPES.keys(),
		)
		
		self.render_to_response('agency_edit.html', context)


class AgencyStopsImport(BaseHandler):
	def get(self, agency_id):
		user = users.get_current_user()
		if not user or not users.is_current_user_admin():
			return

		try:
			agency = Agency.get_by_id(int(agency_id))
			if agency is None:
				return self.send_404()
		except:
			return self.send_404()

		context = dict(
			agency_name=agency.name,
			vehicle_types=VEHICLE_TYPES.keys(),
		)
		
		self.render_to_response('agency_stops_import.html', context)

	@csrf_protect
	def post(self, agency_id):
		user = users.get_current_user()
		if not user or not users.is_current_user_admin():
			return

		try:
			agency = Agency.get_by_id(int(agency_id))
			if agency is None:
				return self.send_404()
		except:
			return self.send_404()

		error_msg = None
		ok_msg = None

		# Process the upload
		upload = self.request.POST['upload']
		if upload.file:
			upload_csv = csv.reader(upload.file)
			header = upload_csv.next()
			
			name_f = header.index('name')
			stop_type_f = header.index('stop_type')
			try:
				stop_id_f = header.index('stop_id')
			except:
				stop_id_f = None
			try:
				stop_code_f = header.index('stop_code')
			except:
				stop_code_f = None
			try:
				y_f = header.index('y')
				x_f = header.index('x')
			except:
				y_f = x_f = None
			
			# Start reading rows
			stop_count = 0
			for row in upload_csv:
				name = row[name_f].strip()
				stop_type = row[stop_type_f].strip()
				if stop_type not in VEHICLE_TYPES.keys():
					error_msg = 'stop type invalid (%r)' % (stop_type,)
					break
				try:
					stop_code = row[stop_code_f].strip()
				except:
					stop_code = None
				try:
					stop_id = row[stop_id_f].strip()
				except:
					stop_id = None
				try:
					x = float(row[x_f].strip())
					y = float(row[y_f].strip())
				except:
					x_f = y_f = None
				if x_f is not None:
					point = ndb.GeoPt(y, x)
				else:
					point = None
				
				stop = Stop(
					name=name,
					stop_type=stop_type,
					agency=agency.key,
					gtfs_stop_id=stop_id,
					gtfs_stop_code=stop_code,
					gtfs_point=point,
				)
				stop.put()
				stop_count += 1
			ok_msg = 'imported %d stop(s)' % stop_count
		else:
			error_msg = 'cannot open uploaded file'

		context = dict(
			agency_name=agency.name,
			vehicle_types=VEHICLE_TYPES.keys(),
			error_msg=error_msg,
			ok_msg=ok_msg,
		)
		
		self.render_to_response('agency_stops_import.html', context)


app = webapp2.WSGIApplication([
	webapp2.Route(r'/app/agency/<agency_id:\d+>/', handler=AgencyDetailView, name='agency_detail'),
	webapp2.Route(r'/app/agency/<agency_id:\d+>/<vehicle:\w+>', handler=AgencyReportView, name='agency_report'),
	webapp2.Route(r'/app/report', handler=SubmitReportView, name='report'),
	webapp2.Route(r'/app/edit/agency/<agency_id:\d+>/', handler=AgencyEditView, name='agency_edit'),
	webapp2.Route(r'/app/edit/agency/<agency_id:\d+>/stops_import', handler=AgencyStopsImport, name='agency_stops_import'),
	(r'/app/', AgencyListView),
], debug=False, config=CONFIG)

