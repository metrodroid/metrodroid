
from google.appengine.api import users
from google.appengine.ext import ndb

VEHICLE_TYPES = {
	'bus': dict(icon='bus.png'),
	'ferry': dict(icon='ferry.png'),
	'train': dict(icon='train.png'),
	'tram': dict(icon='tram.png'),
	'unknown': dict(icon='unknown.png'),
}
	

class Agency(ndb.Model):
	name = ndb.StringProperty(required=True)
	location = ndb.StringProperty(required=True)
	vehicle_types = ndb.StringProperty(repeated=True)
	enabled = ndb.BooleanProperty(required=True, default=True)
	
	@property
	def url(self):
		return 'agency/%s/' % (self.key.id(),)

	@property
	def vehicle_types_comma(self):
		return ','.join(self.vehicle_types)


class Stop(ndb.Model):
	name = ndb.StringProperty(required=True)
	stop_type = ndb.StringProperty(required=True)
	agency = ndb.KeyProperty(kind=Agency, required=True)
	gtfs_stop_id = ndb.StringProperty()
	gtfs_stop_code = ndb.StringProperty()
	gtfs_point = ndb.GeoPtProperty()
	
	@property
	def get_id(self):
		return str(self.key.id())


class Profile(ndb.Model):
	user_id = ndb.StringProperty(required=True)
	email = ndb.StringProperty(required=True)
	banned = ndb.BooleanProperty(required=True, default=False)
	
	@classmethod
	def get_or_create(cls):
		"""
		Gets or creates a new profile for the current user.
		
		Returns None if the user is not logged in.
		"""
		user = users.get_current_user()
		if user:
			profile = cls.query(cls.user_id==user.user_id()).get()
			if profile:
				# Profile exists
				if profile.email != user.email():
					profile.email = user.email()
					profile.put()

				return profile
			else:
				# Create new profile
				profile = cls(user_id=user.user_id(), email=user.email())
				profile.put()
				return profile
		else:
			# Not logged in.
			return None


class StopReport(ndb.Model):
	agency = ndb.KeyProperty(kind=Agency, required=True)

	# Used to identify the stop with a known stop.
	stop = ndb.KeyProperty(kind=Stop, required=False)

	# Used to identify the stop with an unknown stop (eg: buses)
	name = ndb.StringProperty()
	stop_type = ndb.StringProperty()

	# ID as used on the card
	card_id = ndb.StringProperty()
	
	# Human readable notes from the user, in case there is need for
	# disambiguation (eg: travelling southbound on bus 123)
	comment = ndb.StringProperty()

	timestamp = ndb.DateTimeProperty(auto_now_add=True)

	# Abuse tracking data
	gae_country = ndb.StringProperty(required=True)
	gae_region = ndb.StringProperty()
	gae_city = ndb.StringProperty()
	gae_ip = ndb.StringProperty(required=True)
	user = ndb.KeyProperty(kind=Profile, required=True)


