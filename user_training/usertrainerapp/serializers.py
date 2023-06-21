from rest_framework import serializers
from usertrainerapp.models import  User, TrainingModule, Review, Otp
from django.contrib.auth.models import Group


class LoginSerializer(serializers.ModelSerializer):
    class Meta:
        model = User
        fields = ('username','email', 'password')


class RegisterSerializer(serializers.ModelSerializer):
    class Meta:
        model = User
        fields = '__all__'
        
class UserUpdateSerializer(serializers.ModelSerializer):
    class Meta:
        model = User
        fields= '__all__'


class TrainingmoduleSerializer(serializers.ModelSerializer):
    class Meta:
        model = TrainingModule
        fields = '__all__'
        

class ReviewSerializer(serializers.ModelSerializer):
    class Meta:
        model = Review
        fields = '__all__'

class UserSerializer(serializers.ModelSerializer):
    modules = TrainingmoduleSerializer(many=True)
    reviews = ReviewSerializer(many=True)

    class Meta:
        model = User
        fields = ('id', 'username', 'email', 'password', 'first_name', 'last_name', 'modules', 'reviews')
        # extra_kwargs = {'password': {'write_only': True}}
        
        # print(group)
        def create(self, validated_data):
            first_name = validated_data['first_name']
            last_name = validated_data['last_name']
            username = validated_data['username']
            email = validated_data['email']
            password = validated_data['password']
            group = Group.objects.filter(name='User')
            print(group)
            try:
                user = User.objects.get(email = email)   
                return "email already registered kindly SignIn"
            except User.DoesNotExist():
                user = User.objects.create(username = username, email = email)
                user.set_password(password)
                user.groups.add(group[0])
                user.save()   
                # return user
