"""add notification fields

Revision ID: 005
Revises: 004
Create Date: 2024-01-20 12:00:00.000000

"""
from alembic import op
import sqlalchemy as sa

# revision identifiers, used by Alembic.
revision = '005'
down_revision = '004'
branch_labels = None
depends_on = None

def upgrade():
    # Add FCM token field for push notifications
    op.add_column('users', sa.Column('fcm_token', sa.String(), nullable=True))
    
    # Add notification preferences field
    op.add_column('users', sa.Column('notifications_enabled', sa.Boolean(), nullable=False, server_default='true'))

def downgrade():
    # Remove notification fields
    op.drop_column('users', 'notifications_enabled')
    op.drop_column('users', 'fcm_token')
