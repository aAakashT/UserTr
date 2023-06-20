from django.contrib import admin
from .models import User,TrainingModule,  Review, Otp
# Register your models here.

admin.site.register([User, TrainingModule, Review, Otp])